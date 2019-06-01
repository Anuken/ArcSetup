package io.anuke.arc.setup;

import io.anuke.arc.ApplicationListener;
import io.anuke.arc.Core;
import io.anuke.arc.backends.lwjgl3.Lwjgl3Graphics;
import io.anuke.arc.collection.Array;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.SpriteBatch;
import io.anuke.arc.scene.Scene;
import io.anuke.arc.scene.Skin;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.arc.setup.DependencyBank.ProjectDependency;
import io.anuke.arc.setup.DependencyBank.ProjectType;
import io.anuke.arc.util.*;

public class UI implements ApplicationListener{
    String[] templates = {"default", "gamejam", "simple"};

    Lwjgl3Graphics graphics = ((Lwjgl3Graphics)Core.graphics);
    ArcSetup setup = new ArcSetup();

    Dialog buildDialog;
    Label buildLabel;

    public UI(){
        Core.batch = new SpriteBatch();
        Core.scene = new Scene(new Skin(Core.files.internal("ui/uiskin.json")));

        Core.input.addProcessor(Core.scene);

        setup.appName = "Test";
        setup.packageName = "test";
        setup.outputDir = "/home/anuke/Projects/Test";
        setup.template = templates[0];
        setup.modules = Array.with(ProjectType.core, ProjectType.desktop, ProjectType.html);
        setup.dependencies = Array.with(ProjectDependency.arc);
        setup.sdkLocation = "/home/anuke/Android/Sdk";
        setup.callback = this::printlog;
    }

    @Override
    public void init(){
        Core.scene.skin.getFont("default").setUseIntegerPositions(true);
        Core.graphics.setContinuousRendering(false);

        Core.scene.table("button", t -> {
            t.defaults().pad(10f);

            t.row();

            t.table(prefs -> {
                float fw = 400;

                prefs.defaults().padTop(8);

                prefs.add("Name: ").left();
                prefs.addField(setup.appName, name -> {
                    setup.appName = name;
                    setup.outputDir = "/home/anuke/Projects/" + name;
                    setup.packageName = name.toLowerCase();
                }).width(fw);
                prefs.row();

                prefs.add("Package: ").left();
                prefs.addField(setup.packageName, name -> setup.packageName = name).update(f -> f.setText(setup.packageName)).width(fw);
                prefs.row();

                prefs.add("Destination: ");
                prefs.addField(setup.outputDir, name -> setup.outputDir = name).update(f -> f.setText(setup.outputDir)).width(fw);
            });

            t.row();

            t.table("button", temp -> {
                temp.marginTop(12).margin(14f).left();
                temp.add("Template:").left().padBottom(6).left();

                temp.row();
                temp.table(groups -> {
                    ButtonGroup<CheckBox> group = new ButtonGroup<>();

                    for(String type : templates){
                        groups.addCheck(Strings.capitalize(type), type.equals(setup.template), b -> setup.template = type)
                        .group(group).pad(4).left().padRight(8).padLeft(0).fill();
                    }
                });
            });

            t.row();

            t.table("button", proj -> {
                proj.marginTop(12).margin(14f).left();

                proj.add("Sub-projects:").left().padBottom(6).left();
                proj.row();

                proj.table(c -> {
                    for(ProjectType type : ProjectType.values()){
                        c.addCheck(Strings.capitalize(type.name()),
                        setup.modules.contains(type), b -> {
                            if(b){
                                setup.modules.add(type);
                            }else{
                                setup.modules.remove(type);
                            }
                        }).pad(4).left().padRight(8).padLeft(0);
                    }
                });

            }).fillX();

            //refer to initial commit to get the extensions menu out

            t.row();

            t.addButton("Generate", this::generate).padTop(10).fill().height(60);
        });

        Core.scene.table(t -> t.top().table("button", h -> h.add("Arc Project Setup").color(Color.CORAL).get().setFontScale(1f)).growX().height(50f));

        Core.scene.table(t -> {
            float sz = 50;

            t.top().right();
            t.marginTop(0).marginRight(0);

            t.addButton("-", graphics.getWindow()::iconifyWindow).size(sz);
            t.addButton("X", Core.app::exit).size(sz);
        });
    }

    void generate(){
        callSetup();
    }

    void callSetup(){
        buildDialog = new Dialog("Project Log", "dialog");
        buildDialog.setFillParent(true);
        buildLabel = new Label("");

        Table inner = new Table().margin(20);
        inner.add(buildLabel).grow().top().left().wrap().get().setAlignment(Align.topLeft, Align.topLeft);

        ScrollPane pane = new ScrollPane(inner);
        pane.setFadeScrollBars(false);

        buildDialog.cont.add(pane).grow().padTop(8);

        new Thread(() -> {
            printlog("Generating app in " + setup.outputDir + "...\n");

            try{
                setup.build();
            }catch(Exception e){
                e.printStackTrace();
                Core.app.post(() -> {
                    buildDialog.hide();
                    Dialog d = new Dialog("Error generating project!", "dialog");
                    d.setFillParent(true);
                    d.cont.pane(p -> p.add(Strings.parseException(e, true)).grow());
                    d.buttons.addButton("oh god", d::hide).size(100f, 50f);
                    d.show();
                });
                return;
            }

            printlog("Done!\n");

            Core.app.post(() -> {
                buildLabel.invalidateHierarchy();
                buildDialog.invalidateHierarchy();
                buildDialog.cont.invalidateHierarchy();
                buildDialog.pack();

                buildDialog.buttons.addButton("OK", buildDialog::hide).width(100f);
                buildDialog.buttons.addButton("Exit", Core.app::exit).width(100f);
            });
        }).start();

        buildDialog.show();
    }

    void printlog(String str){
        System.out.println(str);
        Core.app.post(() -> {
            buildLabel.getText().append(str).append("\n");
            buildLabel.invalidateHierarchy();
            buildLabel.pack();
        });
    }

    @Override
    public void update(){
        Core.graphics.clear(Color.BLACK);
        Core.scene.act();
        Core.scene.draw();
        Time.update();
    }

    @Override
    public void resize(int width, int height){
        Core.scene.resize(width, height);
    }

}
