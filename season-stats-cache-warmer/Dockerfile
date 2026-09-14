FROM public.ecr.aws/aws-cli/aws-cli:2.17.62 AS awscli

# amazonlinux:2 already ships curl and bash, so this stage needs zero package installs -
# it only copies the AWS CLI's self-contained bundle from the official image above.
FROM amazonlinux:2

COPY --from=awscli /usr/local/aws-cli /usr/local/aws-cli
RUN rm -f /usr/local/bin/aws && ln -s /usr/local/aws-cli/v2/2.17.62/dist/aws /usr/local/bin/aws

COPY warm-cache.sh /warm-cache.sh
RUN chmod +x /warm-cache.sh

ENTRYPOINT ["/warm-cache.sh"]
