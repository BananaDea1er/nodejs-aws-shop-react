package com.myorg;

import software.amazon.awscdk.App;
import software.amazon.awscdk.StackProps;

public class CdkJavaApp {
    public static void main(final String[] args) {
        final var app = new App();
        new CdkJavaStack(app, "CdkJavaStack", StackProps.builder().build());
        app.synth();
    }
}

