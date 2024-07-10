plugins {
    id("java")
}

group = "dev.morazzer"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.nea.moe/releases")
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("moe.nea:neurepoparser:1.5.0")
    implementation("com.google.code.gson:gson:2.11.0")

    implementation("net.kyori:adventure-text-serializer-gson:4.17.0")
    implementation("net.kyori:adventure-text-serializer-legacy:4.17.0")
}

tasks.withType<JavaCompile>().configureEach {
    this.options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
}