FROM public.ecr.aws/sam/build-java21:latest as build-image

ARG SCRATCH_DIR=/var/task/build

COPY src/ src/
COPY gradle/ gradle/
COPY config/ config/
COPY build.gradle gradlew ./

RUN mkdir build
COPY gradle/lambda-build-init.gradle ./build

RUN gradle --project-cache-dir $SCRATCH_DIR/gradle-cache -Dsoftware.amazon.aws.lambdabuilders.scratch-dir=$SCRATCH_DIR --init-script $SCRATCH_DIR/lambda-build-init.gradle build
RUN rm -r $SCRATCH_DIR/gradle-cache
RUN rm -r $SCRATCH_DIR/lambda-build-init.gradle
RUN cp -r $SCRATCH_DIR/*/build/distributions/lambda-build/* .

# Stage to get the fixed libcap RPM package
FROM public.ecr.aws/amazonlinux/amazonlinux:latest as libcap-stage
WORKDIR /rpms
RUN dnf upgrade -y && \
    dnf install -y dnf-plugins-core && \
    dnf download libcap && \
    ls -la libcap*.rpm && \
    dnf clean all

FROM public.ecr.aws/lambda/java:21

# Use a wildcard (*) to copy whatever version was downloaded
COPY --from=libcap-stage /rpms/libcap-*.rpm /tmp/
RUN rpm -Uvh --force /tmp/libcap-*.rpm && \
    rm -f /tmp/libcap-*.rpm && \
    echo "Installed libcap version:" && \
    rpm -qa | grep libcap

COPY --from=build-image /var/task/META-INF ./
COPY --from=build-image /var/task/com/cognizant/lms/userservice ./com/cognizant/lms/userservice
COPY --from=build-image /var/task/lib/ ./lib

COPY --from=public.ecr.aws/datadog/lambda-extension:latest /opt/. /opt/
RUN curl --insecure -Lo dd-java-agent.jar 'https://dtdg.co/latest-java-tracer'

# Command can be overwritten by providing a different command in the template directly.
CMD ["com.cognizant.lms.userservice.StreamLambdaHandler::handleRequest"]

