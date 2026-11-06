package com.kelmy.mediaarchiver;

import static com.kelmy.mediaarchiver.utils.AnsiColors.*;

import java.util.List;

import com.kelmy.mediaarchiver.config.ConfigManager;
import com.kelmy.mediaarchiver.config.ConfigProvider;
import com.kelmy.mediaarchiver.core.ChannelProcessor;
import com.kelmy.mediaarchiver.core.Orchestrator;
import com.kelmy.mediaarchiver.extractors.Extractor;

public class Main {
    public static void main(String[] args) {
        List<Extractor<?, ?>> extractors = List.of();

        ConfigProvider configManager = new ConfigManager();
        ChannelProcessor channelProcessor = new ChannelProcessor(extractors);
        Orchestrator orchestrator = new Orchestrator(configManager, channelProcessor);

        System.out.println(CYAN + "===================================" + RESET);
        System.out.println(BOLD + "    STARTING ARCHIVING ROUTINE" + RESET);
        System.out.println(CYAN + "===================================" + RESET);

        orchestrator.run();

        System.out.println(CYAN + "===================================" + RESET);
        System.out.println(BOLD + "    ARCHIVING ROUTINE COMPLETED " + RESET);
        System.out.println(CYAN + "===================================" + RESET);
    }
}