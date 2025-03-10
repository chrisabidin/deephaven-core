// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: deephaven/proto/ticket.proto

#include "deephaven/proto/ticket.pb.h"

#include <algorithm>

#include <google/protobuf/io/coded_stream.h>
#include <google/protobuf/extension_set.h>
#include <google/protobuf/wire_format_lite.h>
#include <google/protobuf/descriptor.h>
#include <google/protobuf/generated_message_reflection.h>
#include <google/protobuf/reflection_ops.h>
#include <google/protobuf/wire_format.h>
// @@protoc_insertion_point(includes)
#include <google/protobuf/port_def.inc>

PROTOBUF_PRAGMA_INIT_SEG
namespace io {
namespace deephaven {
namespace proto {
namespace backplane {
namespace grpc {
constexpr Ticket::Ticket(
  ::PROTOBUF_NAMESPACE_ID::internal::ConstantInitialized)
  : ticket_(&::PROTOBUF_NAMESPACE_ID::internal::fixed_address_empty_string){}
struct TicketDefaultTypeInternal {
  constexpr TicketDefaultTypeInternal()
    : _instance(::PROTOBUF_NAMESPACE_ID::internal::ConstantInitialized{}) {}
  ~TicketDefaultTypeInternal() {}
  union {
    Ticket _instance;
  };
};
PROTOBUF_ATTRIBUTE_NO_DESTROY PROTOBUF_CONSTINIT TicketDefaultTypeInternal _Ticket_default_instance_;
}  // namespace grpc
}  // namespace backplane
}  // namespace proto
}  // namespace deephaven
}  // namespace io
static ::PROTOBUF_NAMESPACE_ID::Metadata file_level_metadata_deephaven_2fproto_2fticket_2eproto[1];
static constexpr ::PROTOBUF_NAMESPACE_ID::EnumDescriptor const** file_level_enum_descriptors_deephaven_2fproto_2fticket_2eproto = nullptr;
static constexpr ::PROTOBUF_NAMESPACE_ID::ServiceDescriptor const** file_level_service_descriptors_deephaven_2fproto_2fticket_2eproto = nullptr;

const ::PROTOBUF_NAMESPACE_ID::uint32 TableStruct_deephaven_2fproto_2fticket_2eproto::offsets[] PROTOBUF_SECTION_VARIABLE(protodesc_cold) = {
  ~0u,  // no _has_bits_
  PROTOBUF_FIELD_OFFSET(::io::deephaven::proto::backplane::grpc::Ticket, _internal_metadata_),
  ~0u,  // no _extensions_
  ~0u,  // no _oneof_case_
  ~0u,  // no _weak_field_map_
  ~0u,  // no _inlined_string_donated_
  PROTOBUF_FIELD_OFFSET(::io::deephaven::proto::backplane::grpc::Ticket, ticket_),
};
static const ::PROTOBUF_NAMESPACE_ID::internal::MigrationSchema schemas[] PROTOBUF_SECTION_VARIABLE(protodesc_cold) = {
  { 0, -1, -1, sizeof(::io::deephaven::proto::backplane::grpc::Ticket)},
};

static ::PROTOBUF_NAMESPACE_ID::Message const * const file_default_instances[] = {
  reinterpret_cast<const ::PROTOBUF_NAMESPACE_ID::Message*>(&::io::deephaven::proto::backplane::grpc::_Ticket_default_instance_),
};

const char descriptor_table_protodef_deephaven_2fproto_2fticket_2eproto[] PROTOBUF_SECTION_VARIABLE(protodesc_cold) =
  "\n\034deephaven/proto/ticket.proto\022!io.deeph"
  "aven.proto.backplane.grpc\"\030\n\006Ticket\022\016\n\006t"
  "icket\030\001 \001(\014B\004H\001P\001b\006proto3"
  ;
static ::PROTOBUF_NAMESPACE_ID::internal::once_flag descriptor_table_deephaven_2fproto_2fticket_2eproto_once;
const ::PROTOBUF_NAMESPACE_ID::internal::DescriptorTable descriptor_table_deephaven_2fproto_2fticket_2eproto = {
  false, false, 105, descriptor_table_protodef_deephaven_2fproto_2fticket_2eproto, "deephaven/proto/ticket.proto", 
  &descriptor_table_deephaven_2fproto_2fticket_2eproto_once, nullptr, 0, 1,
  schemas, file_default_instances, TableStruct_deephaven_2fproto_2fticket_2eproto::offsets,
  file_level_metadata_deephaven_2fproto_2fticket_2eproto, file_level_enum_descriptors_deephaven_2fproto_2fticket_2eproto, file_level_service_descriptors_deephaven_2fproto_2fticket_2eproto,
};
PROTOBUF_ATTRIBUTE_WEAK const ::PROTOBUF_NAMESPACE_ID::internal::DescriptorTable* descriptor_table_deephaven_2fproto_2fticket_2eproto_getter() {
  return &descriptor_table_deephaven_2fproto_2fticket_2eproto;
}

// Force running AddDescriptors() at dynamic initialization time.
PROTOBUF_ATTRIBUTE_INIT_PRIORITY static ::PROTOBUF_NAMESPACE_ID::internal::AddDescriptorsRunner dynamic_init_dummy_deephaven_2fproto_2fticket_2eproto(&descriptor_table_deephaven_2fproto_2fticket_2eproto);
namespace io {
namespace deephaven {
namespace proto {
namespace backplane {
namespace grpc {

// ===================================================================

class Ticket::_Internal {
 public:
};

Ticket::Ticket(::PROTOBUF_NAMESPACE_ID::Arena* arena,
                         bool is_message_owned)
  : ::PROTOBUF_NAMESPACE_ID::Message(arena, is_message_owned) {
  SharedCtor();
  if (!is_message_owned) {
    RegisterArenaDtor(arena);
  }
  // @@protoc_insertion_point(arena_constructor:io.deephaven.proto.backplane.grpc.Ticket)
}
Ticket::Ticket(const Ticket& from)
  : ::PROTOBUF_NAMESPACE_ID::Message() {
  _internal_metadata_.MergeFrom<::PROTOBUF_NAMESPACE_ID::UnknownFieldSet>(from._internal_metadata_);
  ticket_.UnsafeSetDefault(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited());
  if (!from._internal_ticket().empty()) {
    ticket_.Set(::PROTOBUF_NAMESPACE_ID::internal::ArenaStringPtr::EmptyDefault{}, from._internal_ticket(), 
      GetArenaForAllocation());
  }
  // @@protoc_insertion_point(copy_constructor:io.deephaven.proto.backplane.grpc.Ticket)
}

void Ticket::SharedCtor() {
ticket_.UnsafeSetDefault(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited());
}

Ticket::~Ticket() {
  // @@protoc_insertion_point(destructor:io.deephaven.proto.backplane.grpc.Ticket)
  if (GetArenaForAllocation() != nullptr) return;
  SharedDtor();
  _internal_metadata_.Delete<::PROTOBUF_NAMESPACE_ID::UnknownFieldSet>();
}

inline void Ticket::SharedDtor() {
  GOOGLE_DCHECK(GetArenaForAllocation() == nullptr);
  ticket_.DestroyNoArena(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited());
}

void Ticket::ArenaDtor(void* object) {
  Ticket* _this = reinterpret_cast< Ticket* >(object);
  (void)_this;
}
void Ticket::RegisterArenaDtor(::PROTOBUF_NAMESPACE_ID::Arena*) {
}
void Ticket::SetCachedSize(int size) const {
  _cached_size_.Set(size);
}

void Ticket::Clear() {
// @@protoc_insertion_point(message_clear_start:io.deephaven.proto.backplane.grpc.Ticket)
  ::PROTOBUF_NAMESPACE_ID::uint32 cached_has_bits = 0;
  // Prevent compiler warnings about cached_has_bits being unused
  (void) cached_has_bits;

  ticket_.ClearToEmpty();
  _internal_metadata_.Clear<::PROTOBUF_NAMESPACE_ID::UnknownFieldSet>();
}

const char* Ticket::_InternalParse(const char* ptr, ::PROTOBUF_NAMESPACE_ID::internal::ParseContext* ctx) {
#define CHK_(x) if (PROTOBUF_PREDICT_FALSE(!(x))) goto failure
  while (!ctx->Done(&ptr)) {
    ::PROTOBUF_NAMESPACE_ID::uint32 tag;
    ptr = ::PROTOBUF_NAMESPACE_ID::internal::ReadTag(ptr, &tag);
    switch (tag >> 3) {
      // bytes ticket = 1;
      case 1:
        if (PROTOBUF_PREDICT_TRUE(static_cast<::PROTOBUF_NAMESPACE_ID::uint8>(tag) == 10)) {
          auto str = _internal_mutable_ticket();
          ptr = ::PROTOBUF_NAMESPACE_ID::internal::InlineGreedyStringParser(str, ptr, ctx);
          CHK_(ptr);
        } else
          goto handle_unusual;
        continue;
      default:
        goto handle_unusual;
    }  // switch
  handle_unusual:
    if ((tag == 0) || ((tag & 7) == 4)) {
      CHK_(ptr);
      ctx->SetLastTag(tag);
      goto message_done;
    }
    ptr = UnknownFieldParse(
        tag,
        _internal_metadata_.mutable_unknown_fields<::PROTOBUF_NAMESPACE_ID::UnknownFieldSet>(),
        ptr, ctx);
    CHK_(ptr != nullptr);
  }  // while
message_done:
  return ptr;
failure:
  ptr = nullptr;
  goto message_done;
#undef CHK_
}

::PROTOBUF_NAMESPACE_ID::uint8* Ticket::_InternalSerialize(
    ::PROTOBUF_NAMESPACE_ID::uint8* target, ::PROTOBUF_NAMESPACE_ID::io::EpsCopyOutputStream* stream) const {
  // @@protoc_insertion_point(serialize_to_array_start:io.deephaven.proto.backplane.grpc.Ticket)
  ::PROTOBUF_NAMESPACE_ID::uint32 cached_has_bits = 0;
  (void) cached_has_bits;

  // bytes ticket = 1;
  if (!this->_internal_ticket().empty()) {
    target = stream->WriteBytesMaybeAliased(
        1, this->_internal_ticket(), target);
  }

  if (PROTOBUF_PREDICT_FALSE(_internal_metadata_.have_unknown_fields())) {
    target = ::PROTOBUF_NAMESPACE_ID::internal::WireFormat::InternalSerializeUnknownFieldsToArray(
        _internal_metadata_.unknown_fields<::PROTOBUF_NAMESPACE_ID::UnknownFieldSet>(::PROTOBUF_NAMESPACE_ID::UnknownFieldSet::default_instance), target, stream);
  }
  // @@protoc_insertion_point(serialize_to_array_end:io.deephaven.proto.backplane.grpc.Ticket)
  return target;
}

size_t Ticket::ByteSizeLong() const {
// @@protoc_insertion_point(message_byte_size_start:io.deephaven.proto.backplane.grpc.Ticket)
  size_t total_size = 0;

  ::PROTOBUF_NAMESPACE_ID::uint32 cached_has_bits = 0;
  // Prevent compiler warnings about cached_has_bits being unused
  (void) cached_has_bits;

  // bytes ticket = 1;
  if (!this->_internal_ticket().empty()) {
    total_size += 1 +
      ::PROTOBUF_NAMESPACE_ID::internal::WireFormatLite::BytesSize(
        this->_internal_ticket());
  }

  return MaybeComputeUnknownFieldsSize(total_size, &_cached_size_);
}

const ::PROTOBUF_NAMESPACE_ID::Message::ClassData Ticket::_class_data_ = {
    ::PROTOBUF_NAMESPACE_ID::Message::CopyWithSizeCheck,
    Ticket::MergeImpl
};
const ::PROTOBUF_NAMESPACE_ID::Message::ClassData*Ticket::GetClassData() const { return &_class_data_; }

void Ticket::MergeImpl(::PROTOBUF_NAMESPACE_ID::Message* to,
                      const ::PROTOBUF_NAMESPACE_ID::Message& from) {
  static_cast<Ticket *>(to)->MergeFrom(
      static_cast<const Ticket &>(from));
}


void Ticket::MergeFrom(const Ticket& from) {
// @@protoc_insertion_point(class_specific_merge_from_start:io.deephaven.proto.backplane.grpc.Ticket)
  GOOGLE_DCHECK_NE(&from, this);
  ::PROTOBUF_NAMESPACE_ID::uint32 cached_has_bits = 0;
  (void) cached_has_bits;

  if (!from._internal_ticket().empty()) {
    _internal_set_ticket(from._internal_ticket());
  }
  _internal_metadata_.MergeFrom<::PROTOBUF_NAMESPACE_ID::UnknownFieldSet>(from._internal_metadata_);
}

void Ticket::CopyFrom(const Ticket& from) {
// @@protoc_insertion_point(class_specific_copy_from_start:io.deephaven.proto.backplane.grpc.Ticket)
  if (&from == this) return;
  Clear();
  MergeFrom(from);
}

bool Ticket::IsInitialized() const {
  return true;
}

void Ticket::InternalSwap(Ticket* other) {
  using std::swap;
  auto* lhs_arena = GetArenaForAllocation();
  auto* rhs_arena = other->GetArenaForAllocation();
  _internal_metadata_.InternalSwap(&other->_internal_metadata_);
  ::PROTOBUF_NAMESPACE_ID::internal::ArenaStringPtr::InternalSwap(
      &::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(),
      &ticket_, lhs_arena,
      &other->ticket_, rhs_arena
  );
}

::PROTOBUF_NAMESPACE_ID::Metadata Ticket::GetMetadata() const {
  return ::PROTOBUF_NAMESPACE_ID::internal::AssignDescriptors(
      &descriptor_table_deephaven_2fproto_2fticket_2eproto_getter, &descriptor_table_deephaven_2fproto_2fticket_2eproto_once,
      file_level_metadata_deephaven_2fproto_2fticket_2eproto[0]);
}

// @@protoc_insertion_point(namespace_scope)
}  // namespace grpc
}  // namespace backplane
}  // namespace proto
}  // namespace deephaven
}  // namespace io
PROTOBUF_NAMESPACE_OPEN
template<> PROTOBUF_NOINLINE ::io::deephaven::proto::backplane::grpc::Ticket* Arena::CreateMaybeMessage< ::io::deephaven::proto::backplane::grpc::Ticket >(Arena* arena) {
  return Arena::CreateMessageInternal< ::io::deephaven::proto::backplane::grpc::Ticket >(arena);
}
PROTOBUF_NAMESPACE_CLOSE

// @@protoc_insertion_point(global_scope)
#include <google/protobuf/port_undef.inc>
