package com.tranquilai.subscription.exception

class ResourceNotFoundException(message: String) : RuntimeException(message)
class SubscriptionException(message: String) : RuntimeException(message)
class WebhookException(message: String) : RuntimeException(message)
