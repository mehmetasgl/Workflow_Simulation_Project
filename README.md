# Workflow_Simulation_Project
SE 116 Workflow Simulation Project

Overview

This project implements a Discrete Event Workflow Simulation. The system models how jobs move through a sequence of tasks that are executed at different stations. It simulates real-world workflow environments such as banks, hospitals, factories, or government offices.

Each job consists of multiple tasks, and tasks are processed at stations depending on their type and station capability.

Features
File Input

The program reads two files from the command line:

Workflow file (task types, job types, stations)

Job file (job definitions)

It checks for missing files and reports clear error messages.

Workflow Parsing

The system parses:

Task types

Job types

Station configurations

It validates syntax, checks unique IDs, and reports errors with line numbers.

Job Parsing

The program reads job information including:

Job ID

Job type

Start time

Duration

The deadline of a job is calculated as:

deadline = startTime + duration
Job and Station State Tracking

The system keeps track of:

Job states (waiting, executing, completed)

Current tasks of jobs

Tasks waiting or executing at stations

Stations process tasks based on scheduling strategies such as FIFO or Earliest Deadline First.

Event-Based Simulation

The simulation uses an event queue to manage time. Instead of advancing step-by-step, the system jumps directly to the next event, such as:

Job arrival

Task completion

Each event updates and prints the system state.

Performance Metrics

At the end of the simulation the program reports:

Average job tardiness for late jobs

Station utilization (percentage of time a station is active)

Concepts Used

Object-Oriented Programming (OOP)

File parsing and validation

Discrete event simulation

Queue and scheduling systems

Error handling
