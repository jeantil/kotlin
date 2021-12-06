/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MM_GLOBAL_DATA_H
#define RUNTIME_MM_GLOBAL_DATA_H

#include "GlobalsRegistry.hpp"
#include "GC.hpp"
#include "GCScheduler.hpp"
#include "StableRefRegistry.hpp"
#include "ThreadRegistry.hpp"
#include "Utils.hpp"
#include "ExtraObjectDataFactory.hpp"

namespace kotlin {
namespace mm {

// Global (de)initialization is undefined in C++. Use single global singleton to define it for simplicity.
class GlobalData : private Pinned {
public:
    static GlobalData& Instance() noexcept { return instance_; }

    ThreadRegistry& threadRegistry() noexcept { return threadRegistry_; }
    GlobalsRegistry& globalsRegistry() noexcept { return globalsRegistry_; }
    StableRefRegistry& stableRefRegistry() noexcept { return stableRefRegistry_; }
    ExtraObjectDataFactory& extraObjectDataFactory() noexcept { return extraObjectDataFactory_; }
    gc::GC& gc() noexcept { return *gc_; }

private:
    GlobalData();
    ~GlobalData() = delete;

    // This `GlobalData` is never destroyed.
    static GlobalData instance_;

    ThreadRegistry threadRegistry_;
    GlobalsRegistry globalsRegistry_;
    StableRefRegistry stableRefRegistry_;
    ExtraObjectDataFactory extraObjectDataFactory_;
    KStdUniquePtr<gc::GC> gc_;
};

} // namespace mm
} // namespace kotlin

#endif // RUNTIME_MM_GLOBAL_DATA_H
