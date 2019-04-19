package dev.alangomes.springspigot;

import dev.alangomes.springspigot.util.scheduler.SpigotScheduler;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;

@Configuration
@ComponentScan("dev.alangomes.springspigot")
@ConditionalOnClass({Bukkit.class})
class SpringSpigotAutoConfiguration {

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    private boolean initialized = false;

    @EventListener
    void onStartup(ContextRefreshedEvent event) {
        if (initialized) return;
        initialized = true;
        val beans = applicationContext.getBeansOfType(Listener.class).values();
        val server = applicationContext.getBean(Server.class);
        val plugin = applicationContext.getBean(Plugin.class);
        beans.forEach(bean -> server.getPluginManager().registerEvents(bean, plugin));
    }

    @Bean
    @Scope("singleton")
    public TaskScheduler taskScheduler(Plugin plugin, BukkitScheduler scheduler, @Value("${spigot.scheduler.poolSize:1}") int poolSize) {
        val taskScheduler = new SpigotScheduler(plugin, scheduler);
        taskScheduler.setPoolSize(poolSize);
        taskScheduler.initialize();
        return taskScheduler;
    }

    @Bean
    Server serverBean(Plugin plugin) {
        return plugin.getServer();
    }

    @Bean
    Plugin pluginBean(@Value("${spigot.plugin}") String pluginName) {
        return Bukkit.getPluginManager().getPlugin(pluginName);
    }

    @Bean
    BukkitScheduler schedulerBean(Server server) {
        return server.getScheduler();
    }

    @Bean
    @ConditionalOnMissingBean
    public static BeanFactoryPostProcessor scopeBeanFactoryPostProcessor() {
        return new ScopePostProcessor();
    }
}