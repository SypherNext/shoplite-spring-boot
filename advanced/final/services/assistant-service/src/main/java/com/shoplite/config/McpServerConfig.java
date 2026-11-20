package com.shoplite.config;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.shoplite.assistant.CatalogTools;
import com.shoplite.assistant.PolicyTools;

/**
 * The tools ShopLite offers to other AI applications over MCP (Model Context Protocol). The MCP server
 * starter publishes every ToolCallbackProvider bean; these are the same @Tool methods the chat uses.
 */
@Configuration(proxyBeanMethods = false)
public class McpServerConfig {

    @Bean
    ToolCallbackProvider shopliteTools(CatalogTools catalogTools, PolicyTools policyTools) {
        return MethodToolCallbackProvider.builder().toolObjects(catalogTools, policyTools).build();
    }

}
