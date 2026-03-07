resource "aws_cloudfront_response_headers_policy" "custom_headers_policy" {
  name = var.policy_name_for_env

  security_headers_config {
    content_security_policy {
      override = true
      content_security_policy = "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' blob: data: https://*.${local.hosted_zone_name} https://${local.hosted_zone_name} https://dev-lms-course-service-s3.s3.amazonaws.com; font-src 'self'; connect-src 'self' https://dev-lms-event-service-s3.s3.amazonaws.com https://*.${local.hosted_zone_name} https://dev-lms-course-service-s3.s3.amazonaws.com https://sqs.us-east-1.amazonaws.com wss://notificationservice.${local.hosted_zone_name} https://dev-lms-scan-staging.s3.amazonaws.com https://dev-lms-course-enrolment-service-s3.s3.amazonaws.com https://dev-lms-user-service-s3.s3.amazonaws.com; media-src 'self'; object-src 'self'; frame-src 'self' https://cognizant-dev.engine.scorm.com https://onecsitazrapps.cognizant.com https://login.microsoftonline.com https://onecsitaksbcapps.cognizant.com https://onecdevintaksbcapps.cognizant.com https://learner-catalog.${local.hosted_zone_name}; worker-src 'self'; manifest-src 'self'; script-src-elem 'self' https://*.${local.hosted_zone_name}; frame-ancestors 'self' https://${local.hosted_zone_name} https://*.${local.hosted_zone_name};"
    }

    strict_transport_security {
      override           = true
      access_control_max_age_sec = 31536000
      include_subdomains = true
      preload           = true
    }

    content_type_options {
      override = true
    }

    referrer_policy {
      override = false
      referrer_policy = "strict-origin-when-cross-origin"
    }

  }

  custom_headers_config {
    items {
      header   = "X-Tenant-ID"
      override = false
      value    = "https://${local.hosted_zone_name}"
    }

    items {
      header   = "Cache-Control"
      override = false
      value    = "no-cache, no-store, must-revalidate"
    }
  }

  cors_config {
    access_control_allow_credentials = false
    origin_override = false

    access_control_allow_headers {
      items = ["*"]
    }

    access_control_allow_methods {
      items = ["GET", "HEAD", "PUT", "POST", "PATCH", "DELETE", "OPTIONS"]
    }

    access_control_allow_origins {
      items = ["https://${local.hosted_zone_name}"]
    }

    access_control_expose_headers {
      items = []
    }

    access_control_max_age_sec = 600
  }
}