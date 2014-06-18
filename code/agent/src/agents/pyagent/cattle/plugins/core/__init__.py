import marshaller
import event_router
import event_handlers
import api_proxy
from cattle.type_manager import register_type, MARSHALLER, ROUTER
from cattle.type_manager import POST_REQUEST_HANDLER, LIFECYCLE

register_type(MARSHALLER, marshaller.Marshaller())
register_type(ROUTER, event_router.Router())
register_type(POST_REQUEST_HANDLER, event_handlers.PingHandler())
register_type(POST_REQUEST_HANDLER, event_handlers.ConfigUpdateHandler())
register_type(LIFECYCLE, api_proxy.ApiProxy())
