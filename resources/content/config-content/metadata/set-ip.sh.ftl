#!/bin/bash
set -e

<#if serviceSet?seq_contains("metadataService") >
<#list services["metadataService"].nicNames as nic >
ip addr add 169.254.169.254/16 dev ${nic} 2>/dev/null || true
</#list>
</#if>
