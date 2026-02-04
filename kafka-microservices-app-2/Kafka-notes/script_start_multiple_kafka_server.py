import os
import sys
import shutil
import subprocess
from pathlib import Path

def is_windows() -> bool:
    return os.name == "nt"

def kafka_bin_script(kafka_home: Path, name: str) -> Path:
    ext = ".bat" if is_windows() else ".sh"
    return kafka_home / "bin" / f"{name}{ext}"

def run_capture(cmd, *, cwd: Path):
    p = subprocess.run(cmd, cwd=cwd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    if p.returncode != 0:
        print(p.stdout)
        raise RuntimeError(f"Command failed ({p.returncode}): {' '.join(map(str, cmd))}")
    return p.stdout.strip()

def run_best_effort(cmd, *, cwd: Path):
    subprocess.run(cmd, cwd=cwd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

def resolve_kafka_home(argv) -> Path:
    # 1) CLI arg
    if len(argv) >= 2 and argv[1].strip():
        return Path(argv[1]).expanduser().resolve()

    # 2) Env var
    env_home = os.environ.get("KAFKA_HOME", "").strip()
    if env_home:
        return Path(env_home).expanduser().resolve()

    # 3) Prompt
    entered = input("Enter KAFKA_HOME (path to Kafka folder containing bin/ and config/): ").strip()
    if not entered:
        raise RuntimeError("KAFKA_HOME not provided.")
    return Path(entered).expanduser().resolve()

def main():
    kafka_home = resolve_kafka_home(sys.argv)

    if not (kafka_home / "bin").exists():
        print(f"ERROR: Invalid Kafka home: {kafka_home} (missing bin/).")
        sys.exit(1)
    if not (kafka_home / "config").exists():
        print(f"ERROR: Invalid Kafka home: {kafka_home} (missing config/).")
        sys.exit(1)

    server_props = [
        Path("config/server-1.properties"),
        Path("config/server-2.properties"),
        Path("config/server-3.properties"),
    ]
    for p in server_props:
        if not (kafka_home / p).exists():
            print(f"ERROR: Missing {kafka_home / p}.")
            sys.exit(1)

    stop_script = kafka_bin_script(kafka_home, "kafka-server-stop")
    storage_script = kafka_bin_script(kafka_home, "kafka-storage")
    start_script = kafka_bin_script(kafka_home, "kafka-server-start")

    print("Stopping Kafka (best-effort)...")
    run_best_effort([str(stop_script)], cwd=kafka_home)

    temp_dir = Path(os.environ.get("TEMP") or os.environ.get("TMPDIR") or "/tmp")
    data_dirs = [temp_dir / "server-1", temp_dir / "server-2", temp_dir / "server-3"]

    print(f"Cleaning data dirs in: {temp_dir}")
    for d in data_dirs:
        shutil.rmtree(d, ignore_errors=True)

    print("Generating CLUSTER_ID...")
    cluster_id = run_capture([str(storage_script), "random-uuid"], cwd=kafka_home)
    print(f"CLUSTER_ID={cluster_id}")

    dir1 = run_capture([str(storage_script), "random-uuid"], cwd=kafka_home)
    dir2 = run_capture([str(storage_script), "random-uuid"], cwd=kafka_home)
    dir3 = run_capture([str(storage_script), "random-uuid"], cwd=kafka_home)
    print(f"DIR1={dir1}")
    print(f"DIR2={dir2}")
    print(f"DIR3={dir3}")

    controllers = f"1@localhost:9093:{dir1},2@localhost:9095:{dir2},3@localhost:9097:{dir3}"

    print("Formatting storage...")
    for cfg in server_props:
        run_capture(
            [str(storage_script), "format", "-t", cluster_id, "-c", str(cfg), "--initial-controllers", controllers],
            cwd=kafka_home,
        )

    print("Starting brokers...")
    logs = [
        temp_dir / "kafka-server-1.log",
        temp_dir / "kafka-server-2.log",
        temp_dir / "kafka-server-3.log",
    ]

    procs = []
    log_files = []
    for cfg, log_path in zip(server_props, logs):
        log_f = open(log_path, "w", encoding="utf-8")
        log_files.append(log_f)
        p = subprocess.Popen([str(start_script), str(cfg)], cwd=kafka_home, stdout=log_f, stderr=subprocess.STDOUT)
        procs.append((p, log_path))

    for idx, (p, log_path) in enumerate(procs, start=1):
        print(f"server-{idx}: PID={p.pid} log={log_path}")

    print("Done.")
    print("Tip: open the log files to verify startup.")

if __name__ == "__main__":
    main()