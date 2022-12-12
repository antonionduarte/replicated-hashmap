import json
import multiprocessing
import os
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


def execute_experiments(experiments: list[SingleExperiment]):
    for experiment in experiments:
        execute_experiment(experiment)


def execute_experiment(experiment: SingleExperiment):
    # yadda yadda code to execute the experiment with docker yadda yadda 
    return None


def main():
    config_json = json.loads(open(CONFIG_PATH).read())
    config = Config(config_json)
    experiments = prepare_experiments(config)

    execute_experiments(experiments)

main()

