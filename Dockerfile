FROM eclipse-temurin:17-jdk-jammy

ENV DEBIAN_FRONTEND=noninteractive \
    ANDROID_HOME=/opt/android-sdk \
    ANDROID_SDK_ROOT=/opt/android-sdk \
    GRADLE_HOME=/opt/gradle/gradle-8.11.1 \
    PATH=/opt/gradle/gradle-8.11.1/bin:/opt/android-sdk/cmdline-tools/latest/bin:/opt/android-sdk/platform-tools:/opt/android-sdk/build-tools/35.0.0:$PATH

RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates curl unzip xz-utils python3 file && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /opt/android-sdk/cmdline-tools /opt/gradle \
    && curl -L --fail --retry 3 -o /tmp/cmdline.zip https://dl.google.com/android/repository/commandlinetools-linux-15859902_latest.zip \
    && echo "4e4c464f145a7512b57d088ac6c278c03c9eea610886b35a5e0804e74eedf583  /tmp/cmdline.zip" | sha256sum -c - \
    && unzip -q /tmp/cmdline.zip -d /opt/android-sdk/cmdline-tools \
    && mv /opt/android-sdk/cmdline-tools/cmdline-tools /opt/android-sdk/cmdline-tools/latest \
    && yes | sdkmanager --licenses >/dev/null || true

RUN sdkmanager "platforms;android-35" "build-tools;35.0.0" "platform-tools"

RUN curl -L --fail --retry 3 -o /tmp/gradle.zip https://services.gradle.org/distributions/gradle-8.11.1-bin.zip \
    && unzip -q /tmp/gradle.zip -d /opt/gradle

WORKDIR /builder
COPY yitaptap-build/source.part00 /builder/source.part00

RUN mkdir -p /project /out \
    && cat /builder/source.part00 | base64 --decode > /tmp/yitaptap-source.tar.xz \
    && xz --test /tmp/yitaptap-source.tar.xz \
    && tar -xJf /tmp/yitaptap-source.tar.xz -C /project \
    && gradle -p /project --no-daemon :app:assembleRelease --stacktrace \
    && test -s /project/app/build/outputs/apk/release/app-release-unsigned.apk \
    && keytool -genkeypair -noprompt -keystore /out/YiTapTap-signing.jks -storepass yitaptap-local-100 -keypass yitaptap-local-100 -alias yitaptap -keyalg RSA -keysize 3072 -validity 10000 -dname "CN=YiTapTap,O=New Age Coding Organization,L=Mexico City,C=MX" \
    && zipalign -f -p 4 /project/app/build/outputs/apk/release/app-release-unsigned.apk /tmp/YiTapTap-aligned.apk \
    && apksigner sign --ks /out/YiTapTap-signing.jks --ks-key-alias yitaptap --ks-pass pass:yitaptap-local-100 --key-pass pass:yitaptap-local-100 --out /out/YiTapTap.apk /tmp/YiTapTap-aligned.apk \
    && apksigner verify --verbose --print-certs /out/YiTapTap.apk \
    && unzip -t /out/YiTapTap.apk \
    && sha256sum /out/YiTapTap.apk > /out/YiTapTap.apk.sha256 \
    && sha256sum /out/YiTapTap-signing.jks > /out/YiTapTap-signing.jks.sha256 \
    && printf '%s\n' 'YiTapTap 1.0.0' 'Package: org.newagecoding.yitaptap' 'Alias: yitaptap' 'Keystore password: yitaptap-local-100' 'Key password: yitaptap-local-100' > /out/signing-info.txt

EXPOSE 8080
CMD ["sh", "-c", "python3 -m http.server ${PORT:-8080} --directory /out"]
