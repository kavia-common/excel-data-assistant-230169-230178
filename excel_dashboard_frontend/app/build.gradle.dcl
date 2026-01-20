androidApplication {
    namespace = "org.example.app"

    dependencies {
        implementation("org.apache.commons:commons-text:1.11.0")
        implementation(project(":utilities"))

        // Excel import (.xlsx) via Apache POI (OOXML)
        implementation("org.apache.poi:poi-ooxml:5.2.5")
        // Some Android toolchains require explicit XML dependencies when using POI/OOXML
        implementation("org.apache.xmlbeans:xmlbeans:5.2.0")
        implementation("org.apache.commons:commons-compress:1.26.0")

        // UI: classic Views spreadsheet grid
        implementation("androidx.recyclerview:recyclerview:1.3.2")
    }
}
