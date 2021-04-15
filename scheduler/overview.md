# Non Blocking Task Scheduler

## Overview
This [github](https://github.com/Maps-Messaging/non_block_task_scheduler) project provides a simple task driven mechanism that removes the requirements to block threads while synchronising on structures while changes are done.

The problem faced in [MapsMessaging](index.html) is that the messaging engine needs to update structures that are bound to topics or queues, known as resources. These include things like

* Publication of messages to the resource
* Subscription state on the resource for each individual subscription
* Monitoring of in-flight events, at rest events

The subscription state expands out very rapidly when we count in features like

* Transactional state of the event within the context of the subscription
* State of the subscription on client disconnection or reconnection
* Managing in-flight event state and managing acknowledgements or rollbacks of events within the subscriptions
* Event interest across all subscriptions, as in, is there any interest left for this event or can it be removed from the resource

Things get busy very fast, and the resource becomes the center for [thread contention](https://en.wikipedia.org/wiki/Resource_contention) and [memory contention](https://en.wikipedia.org/wiki/Cache_hierarchy). 
To alleviate this contention the task queue requires that each change to the resource is performed by a task. 
These tasks are queued on the resource and by using the [java future api](https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/Future.html) requesting threads can determine when the task has been executed.

## Tasks

The tasks queued are typically small atomic operations required to be performed on the structure, in this instance, the resource. 
This could be as simple as writing a new event to the resource, checking the size or as complex as adding a new subscription to the resource and starting the event
feed to the client.

The key is that they are small and [atomic](https://en.wikipedia.org/wiki/Linearizability) in nature. This simplifies the understanding of the task queue in that a single task performs one operation only it either completes successfully all of its operations or fails in such a way that any changes are rolled back.

## Task Scheduling

The scheduling of tasks is where it ensures task order, ensures an atomicity of the tasks executed and management of any required threads to perform the tasks.

### Operation

The Task Queue has no threads allocated to it, it is simply a queue that can have tasks added concurrently.

The scheduler comes into play when a task is scheduled and there are a few cases

* No preexisting tasks in the queue
* Existing tasks in the queue
* Existing tasks have exceeded a configured threshold


If the task is the first to be queued then the thread that is queueing the task is used to execute the task helping facilitate CPU cache hits as the same thread is not typically descheduled by the OS since there is no locking in place.
If while the first task is busy being executed another thread queues a task then this task is queued, and the thread returns out of the call. It is up to the caller to either wait for the future to complete or do something else if they do not care about the outcome.

When the first task has finished executing the original thread now checks to see if there are more tasks to execute and will continue to execute tasks up to a configured count. 
If task execute count exceeds the configured limit for "captured" threads then it will schedule a worker thread to take its place and exit out of the scheduler.
The worker thread will continue until there are no more tasks to execute.

The diagram below is a simple overview of the task queue. ThreadA submits a task as does ThreadB. ThreadA was in first so starts the execution of all tasks queued.

[![](https://mermaid.ink/img/eyJjb2RlIjoic2VxdWVuY2VEaWFncmFtXG5UaHJlYWRBLT4-K1Rhc2tTY2hlZHVsZXI6IFNjaGVkdWxlIFRhc2tcblRocmVhZEItPj4rVGFza1NjaGVkdWxlcjogU2NoZWR1bGUgVGFza1xuVGFza1NjaGVkdWxlci0-Pi1UaHJlYWRCOiAgVGFzayBRdWV1ZWRcbmxvb3AgU2NoZWR1bGVyOlF1ZXVlOnBvcCgpXG4gICAgVGFza1NjaGVkdWxlci0-PlRhc2s6IHRhc2sucnVuKClcbmVuZCAgICBcblRhc2tTY2hlZHVsZXItPj4tVGhyZWFkQTogIFRhc2tzIGNvbXBsZXRlZFxuXG5cbiIsIm1lcm1haWQiOnsidGhlbWUiOiJkZWZhdWx0In0sInVwZGF0ZUVkaXRvciI6ZmFsc2V9)](https://mermaid-js.github.io/mermaid-live-editor/#/edit/eyJjb2RlIjoic2VxdWVuY2VEaWFncmFtXG5UaHJlYWRBLT4-K1Rhc2tTY2hlZHVsZXI6IFNjaGVkdWxlIFRhc2tcblRocmVhZEItPj4rVGFza1NjaGVkdWxlcjogU2NoZWR1bGUgVGFza1xuVGFza1NjaGVkdWxlci0-Pi1UaHJlYWRCOiAgVGFzayBRdWV1ZWRcbmxvb3AgU2NoZWR1bGVyOlF1ZXVlOnBvcCgpXG4gICAgVGFza1NjaGVkdWxlci0-PlRhc2s6IHRhc2sucnVuKClcbmVuZCAgICBcblRhc2tTY2hlZHVsZXItPj4tVGhyZWFkQTogIFRhc2tzIGNvbXBsZXRlZFxuXG5cbiIsIm1lcm1haWQiOnsidGhlbWUiOiJkZWZhdWx0In0sInVwZGF0ZUVkaXRvciI6ZmFsc2V9)

Please Note: That ThreadB will return before the task has been executed, this is why it uses Java Futures.