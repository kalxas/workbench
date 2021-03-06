package eu.slipo.workbench.rpc.config;

import java.nio.file.Files;
import java.nio.file.Path;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

import eu.slipo.workflows.WorkflowBuilderFactory;

@Configuration
public class WorkflowBuilderFactoryConfiguration
{
    @Value("#{T(java.nio.file.Paths).get('${slipo.rpc-server.workflows.data-dir}')}")
    private Path dataDir;
    
    @PostConstruct
    private void initialize()
    {
        Assert.isTrue(dataDir.isAbsolute(), 
            "Expected an absolute directory");
        Assert.isTrue(Files.isDirectory(dataDir) && Files.isWritable(dataDir), 
            "Expected a writable directory as a parent data directory");
    }
    
    @Bean
    Path workflowDataDirectory()
    {
        return dataDir;
    }
    
    @Bean
    WorkflowBuilderFactory workflowBuilderFactory()
    {
        WorkflowBuilderFactory factory = new WorkflowBuilderFactory();
        factory.setDataDirectory(dataDir);
        return factory;
    }
}