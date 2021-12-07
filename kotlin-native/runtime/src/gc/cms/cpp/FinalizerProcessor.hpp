/*
* Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
* that can be found in the LICENSE file.
*/

#pragma once

#include "ObjectFactory.hpp"
#include "ConcurrentMarkAndSweep.hpp"
#include "GCState.hpp"

namespace kotlin::gc {
class FinalizerProcessor : Pinned {
public:
    using Queue = typename kotlin::mm::ObjectFactory<ConcurrentMarkAndSweep>::FinalizerQueue;
    explicit FinalizerProcessor(GCStateHolder &state): state_(state) {}
    void ScheduleTasks(Queue&& tasks) noexcept;
    void StopFinalizerThreadForTests() noexcept;
    bool IsRunning() noexcept;
    ~FinalizerProcessor();
private:
    void StartFinalizerThreadIfNone() noexcept;
    std::thread finalizerThread_;
    Queue finalizerQueue_;
    std::mutex finalizerQueueMutex_;
    GCStateHolder& state_;
};
}