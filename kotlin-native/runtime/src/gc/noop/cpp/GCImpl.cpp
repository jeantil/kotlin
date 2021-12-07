/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "GCImpl.hpp"

#include "GC.hpp"
#include "GCScheduler.hpp"
#include "ObjectFactory.hpp"
#include "NoOpGC.hpp"

using namespace kotlin;

namespace {

class GCImpl;

class GCThreadDataImpl : public gc::GC::ThreadData {
public:
    static GCThreadDataImpl& From(gc::GC::ThreadData& threadData) noexcept { return static_cast<GCThreadDataImpl&>(threadData); }

    GCThreadDataImpl(GCImpl& gc, mm::ThreadData& threadData) noexcept;

    gc::NoOpGC::ThreadData& gc() noexcept { return gc_; }

    ObjHeader* CreateObject(const TypeInfo* typeInfo) noexcept override { return objectFactoryThreadQueue_.CreateObject(typeInfo); }

    ArrayHeader* CreateArray(const TypeInfo* typeInfo, uint32_t elements) noexcept override {
        return objectFactoryThreadQueue_.CreateArray(typeInfo, elements);
    }

    void Publish() noexcept override { objectFactoryThreadQueue_.Publish(); }

    void ClearForTests() noexcept override { objectFactoryThreadQueue_.ClearForTests(); }

    void PerformFullGC() noexcept override {}

    void OnStoppedForGC() noexcept override {}

private:
    gc::NoOpGC::ThreadData gc_;
    gc::ObjectFactory<gc::NoOpGC>::ThreadQueue objectFactoryThreadQueue_;
};

class GCImpl : public gc::GC {
public:
    static GCImpl& From(gc::GC& gc) noexcept { return static_cast<GCImpl&>(gc); }

    void ClearForTests() noexcept override { objectFactory_.ClearForTests(); }

    KStdUniquePtr<gc::GC::ThreadData> CreateThreadData(mm::ThreadData& threadData) noexcept override {
        return make_unique<GCThreadDataImpl>(*this, threadData);
    }

    gc::GCSchedulerConfig& GCSchedulerConfig() noexcept override { return gcScheduler_.config(); }

    gc::ObjectFactory<gc::NoOpGC>& objectFactory() noexcept { return objectFactory_; }
    gc::GCScheduler& gcScheduler() noexcept { return gcScheduler_; }
    gc::NoOpGC& gc() noexcept { return gc_; }

private:
    gc::ObjectFactory<gc::NoOpGC> objectFactory_;
    gc::GCScheduler gcScheduler_;
    gc::NoOpGC gc_;
};

GCThreadDataImpl::GCThreadDataImpl(GCImpl& gc, mm::ThreadData& threadData) noexcept : objectFactoryThreadQueue_(gc.objectFactory(), gc_) {}

} // namespace

KStdUniquePtr<gc::GC> gc::CreateGC() noexcept {
    return make_unique<GCImpl>();
}

ALWAYS_INLINE void gc::SafePointFunctionPrologue(mm::ThreadData& threadData) noexcept {}

ALWAYS_INLINE void gc::SafePointLoopBody(mm::ThreadData& threadData) noexcept {}

gc::ObjectFactory<gc::NoOpGC>& gc::GetObjectFactory(gc::GC& gc) noexcept {
    auto& gcImpl = GCImpl::From(gc);
    return gcImpl.objectFactory();
}
