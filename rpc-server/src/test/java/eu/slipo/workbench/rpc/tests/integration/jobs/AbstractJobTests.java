package eu.slipo.workbench.rpc.tests.integration.jobs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.test.AssertFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

public abstract class AbstractJobTests
{
    protected static class Fixture
    {
        final Path inputDir;

        final Path resultsDir;

        final JobParameters parameters;

        Fixture(Path inputDir, Path expectedResultsDir, Map<Object, Object> parametersMap)
            throws IOException
        {
            Assert.notNull(inputDir, "Expected a non-null input directory");
            Assert.isTrue(Files.isDirectory(inputDir) && Files.isReadable(inputDir),
                "Expected a readable input directory");
            this.inputDir = inputDir;

            Assert.notNull(expectedResultsDir, "Expected a non-null input directory");
            Assert.isTrue(
                Files.isDirectory(expectedResultsDir) && Files.isReadable(expectedResultsDir),
                "Expected a readable directory of expected results");
            this.resultsDir = expectedResultsDir;

            JobParametersBuilder parametersBuilder = new JobParametersBuilder();
            parametersBuilder.addString("_id", Long.toHexString(System.currentTimeMillis()));

            parametersMap.forEach((key, value) -> {
                String name = key.toString();
                if (value instanceof Date)
                    parametersBuilder.addDate(name, (Date) value);
                else if (value instanceof Double)
                    parametersBuilder.addDouble(name, (Double) value);
                else if (value instanceof Number)
                    parametersBuilder.addLong(name, ((Number) value).longValue());
                else
                    parametersBuilder.addString(name, value.toString());
            });
            this.parameters = parametersBuilder.toJobParameters();
        }
    }

    protected static Path getResource(String ...pathComponents)
    {
        URL url = AbstractJobTests.class
            .getResource(Paths.get("/testcases", pathComponents).toString());
        return Paths.get(url.getPath());
    }

    @Autowired
    protected JobLauncher jobLauncher;

    @Autowired
    protected JobBuilderFactory jobBuilderFactory;

    @Autowired
    protected Path jobDataDirectory;

    protected abstract String jobName();

    protected abstract String configKey();

    protected abstract Flow jobFlow();

    protected abstract void info(String msg, Object ...args);

    protected abstract void warn(String msg, Object ...args);

    protected abstract Map<String, String> inputParameters(Fixture f) throws Exception;

    protected void testWithFixture(Fixture fixture) throws Exception
    {
        // Build parameters

        final JobParametersBuilder parametersBuilder = new JobParametersBuilder(fixture.parameters);
        this.inputParameters(fixture).forEach(parametersBuilder::addString);

        final JobParameters parameters = parametersBuilder.toJobParameters();

        // Setup listeners

        final CountDownLatch done = new CountDownLatch(1);

        final AtomicReference<Path> workDirReference = new AtomicReference<>();
        final AtomicReference<Path> outputDirReference = new AtomicReference<>();
        final AtomicReference<Path> configFileReference = new AtomicReference<>();

        final String configKey = this.configKey();

        JobExecutionListener listener = new JobExecutionListenerSupport()
        {
            @Override
            public void afterJob(JobExecution jobExecution)
            {
                ExecutionContext executionContext = jobExecution.getExecutionContext();
                if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
                    if (executionContext.containsKey("workDir"))
                        workDirReference.set(Paths.get(executionContext.getString("workDir")));

                    if (executionContext.containsKey("outputDir"))
                        outputDirReference.set(Paths.get(executionContext.getString("outputDir")));

                    if (executionContext.containsKey("configFileByName")) {
                        Map<?,?> configFileByName = (Map<?,?>) executionContext.get("configFileByName");
                        Path configPath = configFileByName == null?
                            null : Paths.get(configFileByName.get(configKey).toString());
                        if (configPath != null)
                            configFileReference.set(configPath);
                    }
                }
                // Done
                done.countDown();
            }
        };

        // Build job from flow

        final String jobName = this.jobName();
        final Flow flow = this.jobFlow();

        Job job = jobBuilderFactory.get(jobName)
            .start(flow)
                .end()
            .listener(listener)
            .build();

        // Launch job and wait

        jobLauncher.run(job, parameters);
        done.await();

        // Check results

        final Path workDir = workDirReference.get();
        assertNotNull(workDir);
        assertTrue(workDir.isAbsolute());
        assertTrue(Files.isDirectory(workDir) && workDir.startsWith(jobDataDirectory));
        info("The job has completed succesfully: workDir={}", workDir);

        final Path configFile = configFileReference.get();
        assertNotNull(configFile);
        assertTrue(!configFile.isAbsolute());
        final Path configPath = workDir.resolve(configFile);
        assertNotNull(configPath);
        assertTrue(Files.isRegularFile(configPath));
        final Path outputDir = outputDirReference.get();
        assertNotNull(outputDir);
        assertTrue(Files.isDirectory(outputDir) && outputDir.startsWith(jobDataDirectory));

        final List<Path> expectedResults = Files.list(fixture.resultsDir).collect(Collectors.toList());
        final List<Path> actualResults = Files.list(outputDir).collect(Collectors.toList());
        assertEquals(expectedResults.size(), actualResults.size());

        for (Path expectedResult: expectedResults) {
            Path fileName = expectedResult.getFileName();
            Path result = outputDir.resolve(fileName);
            AssertFile.assertFileEquals(expectedResult.toFile(), result.toFile());
        }
    }
}
