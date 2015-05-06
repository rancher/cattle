log notify;
path pre_shared_key "/etc/racoon/psk.txt";

remote anonymous
{
	exchange_mode aggressive ;
	my_identifier user_fqdn "${ipsecNetwork.uuid}" ;
    nat_traversal force;
	lifetime time 24 hour ;
	dpd_delay 20 ;
	proposal {
		encryption_algorithm aes;
		hash_algorithm sha1;
		authentication_method pre_shared_key ;
		dh_group 2 ;
	}
}

sainfo anonymous
{
	pfs_group 2;
	lifetime time 12 hour ;
	encryption_algorithm aes ;
	authentication_algorithm hmac_sha1, hmac_md5 ;
	compression_algorithm deflate ;
}
