FROM eclipse-temurin:17-jdk-jammy

ARG YITAPTAP_ICON_B64

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

COPY yitaptap-app /project
RUN set -eux; \
    test -n "$YITAPTAP_ICON_B64"; \
    rm -f /project/app/src/main/res/drawable-nodpi/yitaptap_icon.png; \
    printf '%s' "$YITAPTAP_ICON_B64" | base64 -d > /project/app/src/main/res/drawable-nodpi/yitaptap_icon.webp; \
    file /project/app/src/main/res/drawable-nodpi/yitaptap_icon.webp | grep -q Web/P; \
    gradle -p /project --no-daemon :app:assembleRelease --stacktrace; \
    test -s /project/app/build/outputs/apk/release/app-release-unsigned.apk; \
    aapt2 dump badging /project/app/build/outputs/apk/release/app-release-unsigned.apk | grep -q "package: name='org.newagecoding.yitaptap' versionCode='3' versionName='1.0.2'"

EXPOSE 8080
CMD ["sh", "-c", "set -eu; mkdir -p /out; printf '%s' \"$YITAPTAP_KEYSTORE_B64\" | base64 -d > /tmp/YiTapTap-release.jks; zipalign -f -p 4 /project/app/build/outputs/apk/release/app-release-unsigned.apk /tmp/YiTapTap-aligned.apk; apksigner sign --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled true --ks /tmp/YiTapTap-release.jks --ks-key-alias yitaptap --ks-pass \"pass:$YITAPTAP_KEYSTORE_PASSWORD\" --key-pass \"pass:$YITAPTAP_KEY_PASSWORD\" --out /out/YiTapTap.apk /tmp/YiTapTap-aligned.apk; zipalign -c -p 4 /out/YiTapTap.apk; apksigner verify --verbose --print-certs /out/YiTapTap.apk | tee /out/apksigner.txt; unzip -t /out/YiTapTap.apk; aapt2 dump badging /out/YiTapTap.apk | tee /out/YiTapTap.badging.txt; sha256sum /out/YiTapTap.apk | tee /out/YiTapTap.apk.sha256; python3 -m http.server ${PORT:-8080} --directory /out"]
