#pragma once

#include "GC.hpp"
#include "ObjectFactory.hpp"
#include "NoOpGC.hpp"

namespace kotlin {
namespace gc {

gc::ObjectFactory<gc::NoOpGC>& GetObjectFactory(gc::GC& gc) noexcept;

} // namespace gc
} // namespace kotlin
