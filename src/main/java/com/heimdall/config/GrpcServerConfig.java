package com.heimdall.config;

import io.grpc.ServerInterceptor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.context.annotation.Configuration;

/**
 * gRPC 서버 설정
 */
@Configuration
@Slf4j
public class GrpcServerConfig {

    /**
     * 전역 gRPC 인터셉터
     * 로깅, 인증, 메트릭 수집 등
     */
    @GrpcGlobalServerInterceptor
    ServerInterceptor loggingInterceptor() {
        return new io.grpc.ServerInterceptor() {
            @Override
            public <ReqT, RespT> io.grpc.ServerCall.Listener<ReqT> interceptCall(
                    io.grpc.ServerCall<ReqT, RespT> call,
                    io.grpc.Metadata headers,
                    io.grpc.ServerCallHandler<ReqT, RespT> next) {
                
                log.debug("gRPC call: method={}, headers={}", 
                        call.getMethodDescriptor().getFullMethodName(), headers);
                
                return next.startCall(call, headers);
            }
        };
    }
}
