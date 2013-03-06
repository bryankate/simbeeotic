// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: proto/DSReq.proto

package bbserver.protocol;

public final class BBDatasetRequest {
  private BBDatasetRequest() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
  }
  public static final class DSReq extends
      com.google.protobuf.GeneratedMessage {
    // Use DSReq.newBuilder() to construct.
    private DSReq() {
      initFields();
    }
    private DSReq(boolean noInit) {}
    
    private static final DSReq defaultInstance;
    public static DSReq getDefaultInstance() {
      return defaultInstance;
    }
    
    public DSReq getDefaultInstanceForType() {
      return defaultInstance;
    }
    
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return bbserver.protocol.BBDatasetRequest.internal_static_bbserver_protocol_DSReq_descriptor;
    }
    
    protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return bbserver.protocol.BBDatasetRequest.internal_static_bbserver_protocol_DSReq_fieldAccessorTable;
    }
    
    // required uint32 cmd = 1;
    public static final int CMD_FIELD_NUMBER = 1;
    private boolean hasCmd;
    private int cmd_ = 0;
    public boolean hasCmd() { return hasCmd; }
    public int getCmd() { return cmd_; }
    
    // required uint32 dataset = 2;
    public static final int DATASET_FIELD_NUMBER = 2;
    private boolean hasDataset;
    private int dataset_ = 0;
    public boolean hasDataset() { return hasDataset; }
    public int getDataset() { return dataset_; }
    
    private void initFields() {
    }
    public final boolean isInitialized() {
      if (!hasCmd) return false;
      if (!hasDataset) return false;
      return true;
    }
    
    public void writeTo(com.google.protobuf.CodedOutputStream output)
                        throws java.io.IOException {
      getSerializedSize();
      if (hasCmd()) {
        output.writeUInt32(1, getCmd());
      }
      if (hasDataset()) {
        output.writeUInt32(2, getDataset());
      }
      getUnknownFields().writeTo(output);
    }
    
    private int memoizedSerializedSize = -1;
    public int getSerializedSize() {
      int size = memoizedSerializedSize;
      if (size != -1) return size;
    
      size = 0;
      if (hasCmd()) {
        size += com.google.protobuf.CodedOutputStream
          .computeUInt32Size(1, getCmd());
      }
      if (hasDataset()) {
        size += com.google.protobuf.CodedOutputStream
          .computeUInt32Size(2, getDataset());
      }
      size += getUnknownFields().getSerializedSize();
      memoizedSerializedSize = size;
      return size;
    }
    
    public static bbserver.protocol.BBDatasetRequest.DSReq parseFrom(
        com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return newBuilder().mergeFrom(data).buildParsed();
    }
    public static bbserver.protocol.BBDatasetRequest.DSReq parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return newBuilder().mergeFrom(data, extensionRegistry)
               .buildParsed();
    }
    public static bbserver.protocol.BBDatasetRequest.DSReq parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return newBuilder().mergeFrom(data).buildParsed();
    }
    public static bbserver.protocol.BBDatasetRequest.DSReq parseFrom(
        byte[] data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return newBuilder().mergeFrom(data, extensionRegistry)
               .buildParsed();
    }
    public static bbserver.protocol.BBDatasetRequest.DSReq parseFrom(java.io.InputStream input)
        throws java.io.IOException {
      return newBuilder().mergeFrom(input).buildParsed();
    }
    public static bbserver.protocol.BBDatasetRequest.DSReq parseFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return newBuilder().mergeFrom(input, extensionRegistry)
               .buildParsed();
    }
    public static bbserver.protocol.BBDatasetRequest.DSReq parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      Builder builder = newBuilder();
      if (builder.mergeDelimitedFrom(input)) {
        return builder.buildParsed();
      } else {
        return null;
      }
    }
    public static bbserver.protocol.BBDatasetRequest.DSReq parseDelimitedFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      Builder builder = newBuilder();
      if (builder.mergeDelimitedFrom(input, extensionRegistry)) {
        return builder.buildParsed();
      } else {
        return null;
      }
    }
    public static bbserver.protocol.BBDatasetRequest.DSReq parseFrom(
        com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return newBuilder().mergeFrom(input).buildParsed();
    }
    public static bbserver.protocol.BBDatasetRequest.DSReq parseFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return newBuilder().mergeFrom(input, extensionRegistry)
               .buildParsed();
    }
    
    public static Builder newBuilder() { return Builder.create(); }
    public Builder newBuilderForType() { return newBuilder(); }
    public static Builder newBuilder(bbserver.protocol.BBDatasetRequest.DSReq prototype) {
      return newBuilder().mergeFrom(prototype);
    }
    public Builder toBuilder() { return newBuilder(this); }
    
    public static final class Builder extends
        com.google.protobuf.GeneratedMessage.Builder<Builder> {
      private bbserver.protocol.BBDatasetRequest.DSReq result;
      
      // Construct using bbserver.protocol.BBDatasetRequest.DSReq.newBuilder()
      private Builder() {}
      
      private static Builder create() {
        Builder builder = new Builder();
        builder.result = new bbserver.protocol.BBDatasetRequest.DSReq();
        return builder;
      }
      
      protected bbserver.protocol.BBDatasetRequest.DSReq internalGetResult() {
        return result;
      }
      
      public Builder clear() {
        if (result == null) {
          throw new IllegalStateException(
            "Cannot call clear() after build().");
        }
        result = new bbserver.protocol.BBDatasetRequest.DSReq();
        return this;
      }
      
      public Builder clone() {
        return create().mergeFrom(result);
      }
      
      public com.google.protobuf.Descriptors.Descriptor
          getDescriptorForType() {
        return bbserver.protocol.BBDatasetRequest.DSReq.getDescriptor();
      }
      
      public bbserver.protocol.BBDatasetRequest.DSReq getDefaultInstanceForType() {
        return bbserver.protocol.BBDatasetRequest.DSReq.getDefaultInstance();
      }
      
      public boolean isInitialized() {
        return result.isInitialized();
      }
      public bbserver.protocol.BBDatasetRequest.DSReq build() {
        if (result != null && !isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return buildPartial();
      }
      
      private bbserver.protocol.BBDatasetRequest.DSReq buildParsed()
          throws com.google.protobuf.InvalidProtocolBufferException {
        if (!isInitialized()) {
          throw newUninitializedMessageException(
            result).asInvalidProtocolBufferException();
        }
        return buildPartial();
      }
      
      public bbserver.protocol.BBDatasetRequest.DSReq buildPartial() {
        if (result == null) {
          throw new IllegalStateException(
            "build() has already been called on this Builder.");
        }
        bbserver.protocol.BBDatasetRequest.DSReq returnMe = result;
        result = null;
        return returnMe;
      }
      
      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof bbserver.protocol.BBDatasetRequest.DSReq) {
          return mergeFrom((bbserver.protocol.BBDatasetRequest.DSReq)other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }
      
      public Builder mergeFrom(bbserver.protocol.BBDatasetRequest.DSReq other) {
        if (other == bbserver.protocol.BBDatasetRequest.DSReq.getDefaultInstance()) return this;
        if (other.hasCmd()) {
          setCmd(other.getCmd());
        }
        if (other.hasDataset()) {
          setDataset(other.getDataset());
        }
        this.mergeUnknownFields(other.getUnknownFields());
        return this;
      }
      
      public Builder mergeFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws java.io.IOException {
        com.google.protobuf.UnknownFieldSet.Builder unknownFields =
          com.google.protobuf.UnknownFieldSet.newBuilder(
            this.getUnknownFields());
        while (true) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              this.setUnknownFields(unknownFields.build());
              return this;
            default: {
              if (!parseUnknownField(input, unknownFields,
                                     extensionRegistry, tag)) {
                this.setUnknownFields(unknownFields.build());
                return this;
              }
              break;
            }
            case 8: {
              setCmd(input.readUInt32());
              break;
            }
            case 16: {
              setDataset(input.readUInt32());
              break;
            }
          }
        }
      }
      
      
      // required uint32 cmd = 1;
      public boolean hasCmd() {
        return result.hasCmd();
      }
      public int getCmd() {
        return result.getCmd();
      }
      public Builder setCmd(int value) {
        result.hasCmd = true;
        result.cmd_ = value;
        return this;
      }
      public Builder clearCmd() {
        result.hasCmd = false;
        result.cmd_ = 0;
        return this;
      }
      
      // required uint32 dataset = 2;
      public boolean hasDataset() {
        return result.hasDataset();
      }
      public int getDataset() {
        return result.getDataset();
      }
      public Builder setDataset(int value) {
        result.hasDataset = true;
        result.dataset_ = value;
        return this;
      }
      public Builder clearDataset() {
        result.hasDataset = false;
        result.dataset_ = 0;
        return this;
      }
      
      // @@protoc_insertion_point(builder_scope:bbserver.protocol.DSReq)
    }
    
    static {
      defaultInstance = new DSReq(true);
      bbserver.protocol.BBDatasetRequest.internalForceInit();
      defaultInstance.initFields();
    }
    
    // @@protoc_insertion_point(class_scope:bbserver.protocol.DSReq)
  }
  
  private static com.google.protobuf.Descriptors.Descriptor
    internal_static_bbserver_protocol_DSReq_descriptor;
  private static
    com.google.protobuf.GeneratedMessage.FieldAccessorTable
      internal_static_bbserver_protocol_DSReq_fieldAccessorTable;
  
  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\021proto/DSReq.proto\022\021bbserver.protocol\"%" +
      "\n\005DSReq\022\013\n\003cmd\030\001 \002(\r\022\017\n\007dataset\030\002 \002(\rB\022B" +
      "\020BBDatasetRequest"
    };
    com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
      new com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner() {
        public com.google.protobuf.ExtensionRegistry assignDescriptors(
            com.google.protobuf.Descriptors.FileDescriptor root) {
          descriptor = root;
          internal_static_bbserver_protocol_DSReq_descriptor =
            getDescriptor().getMessageTypes().get(0);
          internal_static_bbserver_protocol_DSReq_fieldAccessorTable = new
            com.google.protobuf.GeneratedMessage.FieldAccessorTable(
              internal_static_bbserver_protocol_DSReq_descriptor,
              new java.lang.String[] { "Cmd", "Dataset", },
              bbserver.protocol.BBDatasetRequest.DSReq.class,
              bbserver.protocol.BBDatasetRequest.DSReq.Builder.class);
          return null;
        }
      };
    com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
        }, assigner);
  }
  
  public static void internalForceInit() {}
  
  // @@protoc_insertion_point(outer_class_scope)
}
