$processorTime=(get-counter "\Processor(_Total)\% Processor Time" -SampleInterval 2).CounterSamples.CookedValue
echo Processor_Time=$processorTime
