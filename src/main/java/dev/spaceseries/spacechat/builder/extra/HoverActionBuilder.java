package dev.spaceseries.spacechat.builder.extra;

import dev.spaceseries.api.config.impl.Configuration;
import dev.spaceseries.spacechat.builder.IBuilder;
import dev.spaceseries.spacechat.model.action.HoverAction;

import java.util.Collections;
import java.util.List;

public class HoverActionBuilder implements IBuilder<Configuration, HoverAction> {

    /**
     * Builds an V (output) from a K (input)
     *
     * @param input The input
     */
    @Override
    public HoverAction build(Configuration input) {
        // create object
        HoverAction hoverAction = new HoverAction();

        // get lines
        List<String> lines = input.getStringList("lines", Collections.emptyList());

        // set lines
        hoverAction.setLines(lines);

        // return
        return hoverAction;
    }
}