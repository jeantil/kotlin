/*
* Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
* that can be found in the LICENSE file.
*/

#include "FinalizerProcessor.hpp"
#include "ObjectFactory.hpp"
#include "Runtime.h"

#include <thread>


void kotlin::gc::FinalizerProcessor::StartFinalizerThreadIfNone() noexcept {
    if (finalizerThread_.joinable()) return;
    finalizerThread_ = std::thread([this] {
        Kotlin_initRuntimeIfNeeded();
        while (true) {
            auto finalizersEpoch = state_.waitFinalizersRequired();
            if (finalizersEpoch == std::numeric_limits<int64_t>::max()) break;
            std::unique_lock lock(finalizerQueueMutex_);
            auto queue = std::move(finalizerQueue_);
            lock.unlock();
            if (queue.size() > 0) {
                ThreadStateGuard guard(ThreadState::kRunnable);
                queue.Finalize();
            }
            state_.finalized(finalizersEpoch);
        }
    });
}

void kotlin::gc::FinalizerProcessor::StopFinalizerThreadForTests() noexcept {
    auto epoch = state_.waitCurrentFinished();
    if (finalizerThread_.joinable()) {
        state_.finish(std::numeric_limits<int64_t>::max());
        finalizerThread_.join();
        RuntimeAssert(finalizerQueue_.size() == 0, "Finalizer queue should be empty when killing finalizer thread");
        state_.finish(epoch);
        state_.finalized(epoch);
    }
}

void kotlin::gc::FinalizerProcessor::ScheduleTasks(kotlin::mm::ObjectFactory<ConcurrentMarkAndSweep>::FinalizerQueue&& tasks) noexcept {
    if (tasks.size() > 0) {
        StartFinalizerThreadIfNone();
        std::unique_lock guard(finalizerQueueMutex_);
        finalizerQueue_.MergeWith(std::move(tasks));
    }
}

bool kotlin::gc::FinalizerProcessor::IsRunning() noexcept {
    return finalizerThread_.joinable();
}

kotlin::gc::FinalizerProcessor::~FinalizerProcessor() {
    if (finalizerThread_.joinable()) {
        finalizerThread_.join();
    }
}