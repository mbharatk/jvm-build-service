package com.redhat.hacbs.analyser.artifactanalysis;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.quarkus.dev.console.StatusLine;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ProgressMonitor;

import com.redhat.hacbs.analyser.config.CheckoutConfig;
import com.redhat.hacbs.analyser.config.RepoConfig;
import com.redhat.hacbs.analyser.data.scm.ScmManager;
import com.redhat.hacbs.analyser.maven.ArtifactAnalyser;
import com.redhat.hacbs.recipies.BuildRecipe;
import com.redhat.hacbs.recipies.location.AddRecipeRequest;
import com.redhat.hacbs.recipies.location.RecipeLayoutManager;
import com.redhat.hacbs.recipies.location.RecipeRepositoryManager;
import com.redhat.hacbs.recipies.scm.ScmInfo;

import io.quarkus.dev.console.QuarkusConsole;
import io.quarkus.logging.Log;
import io.quarkus.runtime.ApplicationLifecycleManager;
import picocli.CommandLine;

@CommandLine.Command(name = "analyse-repositories")
@Singleton
public class AnalyseRepositoriesCommand implements Runnable {

    public static final String CLONE_FAILURE = "CLONE_FAILURE";
    @Inject
    CheckoutConfig checkoutConfig;

    @Inject
    RepoConfig repoConfig;

    @Override
    public void run() {
        var status = QuarkusConsole.INSTANCE.registerStatusLine(100);
        System.out.println("START EVENT " + this.toString());
        try (ScmManager manager = ScmManager.create(repoConfig.path())) {
            RecipeLayoutManager recipeLayoutManager = new RecipeLayoutManager(
                    repoConfig.path().resolve(RecipeRepositoryManager.RECIPES));
            for (var repository : new ArrayList<>(manager.getAll())) {
                if (!isRunning()) {
                    return;
                }
                try {
                    if (repository.isFailed() || repository.isProcessed()) {
                        continue;
                    }
                    if (repository.getType() != null && !"git".equals(repository.getType())) {
                        continue;
                    }
                    if (repository.getUuid() == null) {
                        repository.setUuid(UUID.randomUUID().toString());
                    }
                    Path checkoutPath = checkoutConfig.path().resolve(repository.getUuid());
                    Git checkout;
                    if (Files.exists(checkoutPath)) {
                        checkout = Git.open(checkoutPath.toFile());

                        System.out.println("Using existing " + repository.getUri() + " at " + checkoutPath);
                        //                        var remotes = checkout.remoteList().call();
                        //                        if (!remotes.get(0).getPushURIs().get(0).getHumanishName().equals(repository.getUri())) {
                        //                            System.out.println("FAILED: " + remotes.get(0).getPushURIs().get(0).getHumanishName() + "  " + repository.getUri());
                        //                        }
                    } else {
                        try {
                            System.out.println("Checking out " + repository.getUri() + " into " + checkoutPath);
                            checkout = Git.cloneRepository().setDirectory(checkoutPath.toFile())
                                    .setProgressMonitor(new QuarkusProgressMonitor(status))
                                    .setURI(repository.getUri()).call();
                        } catch (Throwable t) {
                            Log.errorf(t, "Failed to clone %s", repository.getUri());
                            repository.setUuid(null);
                            repository.setFailed(true);
                            repository.setFailedReason(CLONE_FAILURE);
                            manager.writeData();
                            continue;
                        }
                    }
                    try {
                        var result = ArtifactAnalyser.doProjectDiscovery(checkoutPath);
                        for (var module : result.getProjects().values()) {
                            ScmInfo info = new ScmInfo("git", repository.getUri());
                            recipeLayoutManager.writeArtifactData(new AddRecipeRequest<>(BuildRecipe.SCM, info,
                                    module.getGav().getGroupId(), module.getGav().getArtifactId(), null));
                        }
                    } catch (Throwable t) {
                        Log.errorf(t, "Failed to analyse %s", repository.getUri());
                    }

                } catch (Exception e) {
                    Log.errorf(e, "Failed to handle %s", repository.getUri());
                    repository.setFailed(true);
                }
                manager.writeData();
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    boolean isRunning() {
        try {
            Field field = ApplicationLifecycleManager.class.getDeclaredField("exitCode");
            field.setAccessible(true);
            return (Integer) field.get(null) == -1;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class QuarkusProgressMonitor implements ProgressMonitor {
        private final StatusLine status;
        int totalTasks;
        int taskCount;
        String currentTask;
        int currentTotal;
        int current;
        long lastUpdate = System.currentTimeMillis();

        public QuarkusProgressMonitor(StatusLine status) {
            this.status = status;
        }

        @Override
        public void start(int i) {
            totalTasks = i;
            taskCount = 0;
            current = 0;
        }

        @Override
        public void beginTask(String s, int i) {
            taskCount++;
            currentTask = s;
            currentTotal = i;
            current = 0;
            updateText();

        }

        @Override
        public void update(int i) {
            if (currentTask == null) {
                return;
            }
            current += i;
            if (System.currentTimeMillis() > lastUpdate + 1000) {
                updateText();
            }
        }

        @Override
        public void endTask() {
            status.setMessage("");
        }

        private void updateText() {
            lastUpdate = System.currentTimeMillis();
            status.setMessage("[" + taskCount + "/" + totalTasks + "] " + currentTask + " ("
                    + current + "/" + currentTotal + ")");
        }

        @Override
        public boolean isCancelled() {
            return false;
        }
    }
}
