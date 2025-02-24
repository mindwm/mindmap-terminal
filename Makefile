FP_CONFIG_VERSION = 1.12.x
FP_USERDIR_PLUGIN = $(FP_CONFIG_VERSION)/plugins/org.freeplane.plugin.grpc 
FP_GRPC_PLUGIN_URL = https://github.com/metacoma/freeplane_plugin_grpc/releases/download/0.0.7/org.freeplane.plugin.tgz
.ONESHELL: freeplane_grpc_plugin_download freeplane_start
freeplane_grpc_plugin_download:
	test -d $(FP_USERDIR_PLUGIN) || {
		mkdir -p $(FP_USERDIR_PLUGIN)/
		curl -Lso - $(FP_GRPC_PLUGIN_URL) | tar zxvf - -C $(FP_USERDIR_PLUGIN)
	} 	

freeplane_start: freeplane_grpc_plugin_download
	export MINDWM_USER=$${USER}
	export MINDWM_HOST=$$(hostname -s)
	freeplane -U`pwd`
