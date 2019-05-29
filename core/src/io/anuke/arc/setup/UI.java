package io.anuke.arc.setup;

import io.anuke.arc.ApplicationListener;
import io.anuke.arc.Core;
import io.anuke.arc.backends.lwjgl3.Lwjgl3Graphics;
import io.anuke.arc.collection.Array;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.SpriteBatch;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.arc.setup.DependencyBank.ProjectDependency;
import io.anuke.arc.setup.DependencyBank.ProjectType;
import io.anuke.arc.util.Strings;
import io.anuke.arc.util.Time;

public class UI implements ApplicationListener{
	String[] templates = {"default", "gamejam", "simple"};
	
	Lwjgl3Graphics graphics = ((Lwjgl3Graphics)Core.graphics);
	ArcSetup setup = new ArcSetup();
	
	Dialog buildDialog;
	Label buildLabel;

	public UI(){
		setup.appName = "Test";
		setup.packageName = "test";
		setup.outputDir = "/home/anuke/Projects/Test";
		setup.template = templates[0];
		setup.modules = Array.with(ProjectType.core, ProjectType.desktop, ProjectType.html);
		setup.dependencies = Array.with(ProjectDependency.arc);
		setup.sdkLocation = "/home/anuke/Android/Sdk";
		setup.callback = this::printlog;

		Core.batch = new SpriteBatch();
	}
	
	@Override
	public void init(){
		Core.scene.skin.getFont("default").setUseIntegerPositions(true);
		Core.graphics.setContinuousRendering(false);

		Core.scene.table("button", t -> {

			t.row();

			t.table(prefs -> {
				float fw = 400;

				prefs.defaults().padTop(8);

				prefs.add("Name: ").left();
				prefs.addField(setup.appName, name -> setup.appName = name).width(fw);
				prefs.row();

				prefs.add("Package: ").left();
				prefs.addField(setup.packageName, name -> setup.packageName = name).width(fw);
				prefs.row();

				prefs.add("Destination: ");
				prefs.addField(setup.outputDir, name -> setup.outputDir = name).width(fw);
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
						setup.modules.contains(type), b->{
							if(b){
								setup.modules.add(type);
							}else{
								setup.modules.remove(type);
							}
						}).pad(4).left().padRight(8).padLeft(0);
					}
				});

			}).fillX();

			t.row();

			t.table("button", ext -> {
				ext.margin(14);

				ext.add("Extensions:").left().padBottom(6);

				ext.row();

				ProjectDependency[] depend = ProjectDependency.values();
				int amount = ProjectDependency.values().length;
				int max = 5;

				Table current = new Table();

				ext.add(current);

				for(int i = 0 ; i < amount; i ++){
					if(i % max == 0){
						current.row();
					}

					int idx = i;

					current.addCheck(Strings.capitalize(depend[i].name().toLowerCase()),
					setup.dependencies.contains(depend[i]), b -> {
						if(b){
							if(!setup.dependencies.contains(depend[idx])) setup.dependencies.add(depend[idx]);
						}else{
							setup.dependencies.remove(depend[idx]);
						}
					}).left().pad(4).padLeft(0);
				}

			}).fillX();

			t.row();

			t.addButton("Generate", this::generate).padTop(10).fill().height(60);
		});

		Core.scene.table(t -> t.top().add("uCore Project Setup").padTop(12).color(Color.CORAL).get().setFontScale(1f));

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
		buildLabel = new Label("");

		Table inner = new Table().margin(20);
		inner.add(buildLabel);

		ScrollPane pane = new ScrollPane(inner);
		pane.setFadeScrollBars(false);
		
		buildDialog.cont.add(pane).grow().padTop(8);
		
		new Thread(() -> {
			printlog("Generating app in " + setup.outputDir + "...\n");

			try {
				setup.build();
			}catch (Exception e){
				Core.app.post(() -> {
					Dialog d = new Dialog("Error generating project!", "dialog");
					d.cont.add(Strings.parseException(e, true));
					d.addCloseButton();
					d.show();
				});
				return;
			}

			printlog("Done!\n");

			Core.app.post(()->{
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
		buildLabel.getText().append(str);
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
