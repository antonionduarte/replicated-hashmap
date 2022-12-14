import multiprocessing as mp
import sys
import time
import config

from spawn_processes import spawn_replica_java_docker, spawn_ycsb_clients_docker, num_exp_set, clean_docker_environment
from config import SingleExperiment

CONFIG_PATH = "./analysis/config.json"


# Executes multiple experiments one by one
def execute_experiments(experiments: list[SingleExperiment]):
    clean_docker_environment()
    for experiment in experiments:
        print(f"Executing: {experiment.number_clients}C {experiment.number_replicas}R")
        execute_experiment(experiment)


# Executes an experiment with the given experiment configuration
def execute_experiment(experiment: SingleExperiment):
    client = mp.Process(target=worker_spawn_clients, args=(experiment,))

    replica_processes = []
    for i in range(experiment.number_replicas):
        replica_processes.append(
            mp.Process(target=worker_spawn_replica, args=(i, experiment)))

    for process in replica_processes:
        process.start()

    time.sleep(5)
    client.start()

    for process in replica_processes:
        process.join()

    client.join()

    clean_docker_environment()


# Worker function to be called by multiprocessing, handles spawning a replica
def worker_spawn_replica(port: int, config: SingleExperiment):
    spawn_replica_java_docker(port, config)


# Worker function to be called by multiprocessing, handles spawning the YCSB clients
def worker_spawn_clients(config: SingleExperiment):
    spawn_ycsb_clients_docker(config)


if __name__ == "__main__":
    exp_name = "exp"+num_exp_set() if len(sys.argv) == 1 else sys.argv[1]
    experiments = config.prepare_experiments(config.parse_config(CONFIG_PATH), exp_name)
    execute_experiments(experiments)
