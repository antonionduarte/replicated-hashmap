import json
import multiprocessing as mp
import spawn_paxos
from dataclasses import dataclass

CONFIG_PATH = "./config.json"

class Config:
    number_clients: int
    number_replicas: int
    percentage_writes: int 
    percentage_reads: int
    min_clients: int
    max_clients: int

    def __init__(self, config):
        self.min_clients = config['min-clients']
        self.max_clients = config['max-clients']
        self.step = config['step']
        self.number_replicas = config['number-replicas']
        self.percentage_writes = config['percentage-writes']
        self.percentage_reads = config['percentage-reads']


@dataclass
class SingleExperiment:
    number_clients: int 
    number_replicas: int 
    percentage_writes: int 
    percentage_reads: int


# Divide a configuration file into several executable experiments
def prepare_experiments(config: Config) -> list[SingleExperiment]:
    experiments = []
    number_clients = config.min_clients
    while number_clients <= config.max_clients:
        experiments.append(SingleExperiment( 
            config.min_clients,
            config.number_replicas, 
            config.percentage_writes, 
            config.percentage_reads))
        number_clients += config.step
    return experiments


# Executes multiple experiments one by one
def execute_experiments(experiments: list[SingleExperiment]):
    for experiment in experiments:
        execute_experiment(experiment)


# Executes an experiment with the given experiment configuration
def execute_experiment(experiment: SingleExperiment):
    client = mp.Process(target=worker_spawn_clients)

    replica_processes = []
    for i in range(1, experiment.number_replicas):
        replica_processes.append(
            mp.Process(target=worker_spawn_replica, args=(i, experiment.number_replicas)))
        
    client.start()
    for process in replica_processes:
        process.start()

    client.join()
    for process in replica_processes:
        process.join()


# Worker function to be called by multiprocessing, handles spawning a replica
def worker_spawn_replica(port: int, number_replicas: int):
    spawn_paxos.spawn_paxos_java_docker(port, number_replicas)


# Worker function to be called by multiprocessing, handles spawning the YCSB clients
def worker_spawn_clients(number_replicas: int):
    spawn_paxos.spawn_ycsb_clients_docker(number_replicas)
    

if __name__ == "__main__":
    config_json = json.loads(open(CONFIG_PATH).read())
    config = Config(config_json)
    experiments = prepare_experiments(config)

    execute_experiments(experiments)

