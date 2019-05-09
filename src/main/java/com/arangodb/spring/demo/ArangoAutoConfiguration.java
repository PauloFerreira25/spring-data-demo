package com.arangodb.spring.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.arangodb.ArangoDB;
import com.arangodb.ArangoDB.Builder;
import com.arangodb.ArangoDBException;
import com.arangodb.springframework.annotation.EnableArangoRepositories;
import com.arangodb.springframework.boot.autoconfigure.ArangoProperties;
import com.arangodb.springframework.config.ArangoConfiguration;

@Configuration
@Import(TenantProvider.class)
@EnableConfigurationProperties(ArangoProperties.class)
@EnableArangoRepositories(basePackages = { "com.arangodb.spring.demo.repository" })
public class ArangoAutoConfiguration implements ArangoConfiguration {

	private final ArangoProperties properties;
	
	@Autowired
	TenantProvider tenantProvider;

	public ArangoAutoConfiguration(final ArangoProperties properties) {
		super();
		this.properties = properties;
	}

	// Config TenantProvider
	@Override
	public String database() {
		return "#{tenantProvider.getId()}";
	}

	@Override
	public Builder arango() {
		ArangoDB.Builder builder = new ArangoDB.Builder().user(properties.getUser()).password(properties.getPassword())
				.timeout(properties.getTimeout()).useSsl(properties.getUseSsl())
				.maxConnections(properties.getMaxConnections()).connectionTtl(properties.getConnectionTtl())
				.acquireHostList(properties.getAcquireHostList())
				.loadBalancingStrategy(properties.getLoadBalancingStrategy()).useProtocol(properties.getProtocol());
		properties.getHosts().stream().map(this::parseHost)
				.forEach(host -> builder.host(host[0], Integer.valueOf(host[1])));
		// Default Database
		tenantProvider.setId(properties.getDatabase());
		return builder;
	}

	private String[] parseHost(final String host) {
		final String[] split = host.split(":");
		if (split.length != 2 || !split[1].matches("[0-9]+")) {
			throw new ArangoDBException(String.format(
					"Could not load host '%s' from property-value spring.data.arangodb.hosts. Expected format ip:port,ip:port,...",
					host));
		}
		return split;
	}

}
