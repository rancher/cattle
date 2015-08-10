 {"default": {
	"version": "${version}",
	<#if stacks??>
 	"stacks":
 	 	${stacks},
 	</#if>
 	<#if services??>
 	"services":
 		${services},
 	</#if>
 	<#if containers??>
 	"containers":
 		${containers}
 	</#if>
  }
  
  <#if self?? && self?has_content>
  <#list self?keys as key>
  ,"${key}": {
    "self":
    ${self[key]}
    }
  </#list>
  </#if>
  }