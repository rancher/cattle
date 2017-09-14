# Examples to generate some SQL to start defining tables

. tables-common.sh

default_table account
default_table agent_group
default_table zone

start agent
string uri
bool managed_config 1
ref agent_group
ref zone
end agent

start credential
string public_value 4096
string secret_value 4096
end credential

start host
string uri
bigint compute_free
bigint compute_total
ref agent
ref zone
index host compute_free
end host

start image
string url
bool is_public
bigint physical_size_mb
bigint virtual_size_mb
string checksum
string format
end image

start offering
bool is_public
end offering

start instance
string allocation_state
bigint compute
bigint memory_mb
ref image
ref offering
string hostname
ref zone
end instance

start storage_pool
bigint physical_total_size_mb
bigint virtual_total_size_mb
bool external
ref agent
ref zone
end storage_pool

start volume
bigint physical_size_mb
bigint virtual_size_mb
int device_number
string format
string allocation_state
string attached_state
ref instance
ref image
ref offering
ref zone
end volume


map instance host
map image storage_pool
map storage_pool host
map volume storage_pool
