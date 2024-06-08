package com.myorg;

import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.cloudfront.BehaviorOptions;
import software.amazon.awscdk.services.cloudfront.Distribution;
import software.amazon.awscdk.services.cloudfront.OriginAccessIdentity;
import software.amazon.awscdk.services.cloudfront.origins.S3Origin;
import software.amazon.awscdk.services.cloudfront.origins.S3OriginProps;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.s3.BlockPublicAccess;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.BucketProps;
import software.amazon.awscdk.services.s3.deployment.BucketDeployment;
import software.amazon.awscdk.services.s3.deployment.Source;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

public class CdkJavaStack extends Stack {

    private static final String DEFAULT_ROOT_OBJECT = "index.html";
    private static final String ORIGIN_ACCESS_IDENTITY_COMMENT = "OAI for my distribution";
    private static final String AWS_CLOUDFRONT_URL = "cloudfront.amazonaws.com";
    private static final String DIST_LOCATION = "../dist";
    private static final String DISTRIBUTION_PATH_PATTERN = "/*";
    private static final String CLOUDFRONT_DISTRIBUTION_PATTERN = "arn:aws:cloudfront::%s:distribution/%s";

    public CdkJavaStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public CdkJavaStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        final var siteBucket = buildBucket();
        final var oai = OriginAccessIdentity.Builder.create(this, "OAI_new")
                .comment(ORIGIN_ACCESS_IDENTITY_COMMENT)
                .build();
        final var distribution = buildDistribution(oai, siteBucket);

        final var policyPermissions = buildPolicyPermissions(siteBucket, distribution);
        siteBucket.addToResourcePolicy(policyPermissions);
        siteBucket.grantRead(oai);

        initBucketDeployment(id, siteBucket, distribution);
    }

    private Bucket buildBucket() {
        final var siteBucketProperties = BucketProps.builder()
                .removalPolicy(RemovalPolicy.DESTROY)
                .blockPublicAccess(BlockPublicAccess.BLOCK_ALL)
                .autoDeleteObjects(true)
                .build();

        return new Bucket(this, "rs-task-2-bucket", siteBucketProperties);
    }

    private Distribution buildDistribution(
            OriginAccessIdentity oai,
            Bucket siteBucket
    ) {
        final var s3OriginProps = S3OriginProps.builder()
                .originAccessIdentity(oai)
                .build();
        final var s3Origin = new S3Origin(siteBucket, s3OriginProps);
        final var behaviourOptions = BehaviorOptions.builder()
                .origin(s3Origin)
                .build();

        return Distribution.Builder.create(this, "MyStaticDistribution")
                .defaultBehavior(behaviourOptions)
                .defaultRootObject(DEFAULT_ROOT_OBJECT)
                .build();
    }

    private PolicyStatement buildPolicyPermissions(Bucket siteBucket, Distribution distribution) {
        final var s3GetObjectAction = "s3:GetObject";
        final var actions = List.of(s3GetObjectAction);

        final var keyPattern = "*";
        final var resource = siteBucket.arnForObjects(keyPattern);
        final var resources = List.of(resource);

        final var principal = ServicePrincipal.Builder.create(AWS_CLOUDFRONT_URL).build();
        final var principals = List.of(principal);

        final var sourceArnKey = "AWS:SourceArn";
        final var sourceArnValue = CLOUDFRONT_DISTRIBUTION_PATTERN.formatted(getAccount(), distribution.getDistributionId());
        final var sourceArnMap = Map.of(sourceArnKey, sourceArnValue);
        final var stringEqualsConditionKey = "StringEquals";
        final var conditions = Map.of(stringEqualsConditionKey, sourceArnMap);

        return PolicyStatement.Builder.create()
                .actions(actions)
                .resources(resources)
                .principals((principals))
                .conditions(conditions)
                .build();
    }

    private void initBucketDeployment(
            String id,
            Bucket siteBucket,
            Distribution distribution
    ) {
        final var source = Source.asset(DIST_LOCATION);
        final var sources = List.of(source);
        final var distributionPath = List.of(DISTRIBUTION_PATH_PATTERN);
        BucketDeployment.Builder.create(this, id)
                .sources(sources)
                .destinationBucket(siteBucket)
                .distribution(distribution)
                .distributionPaths(distributionPath)
                .build();
    }
}
