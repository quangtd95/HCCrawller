package com.quangtd;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.*;

public class HChargeTkCrawler {
    //
    public static final String baseUrl = "https://hcharge.tk/Heroes";
    private HashSet<String> linkHeroes;
    private ArrayBlockingQueue<String> listHeroesRemain;
    private ArrayBlockingQueue<String> listArtsRemain;
    private HashSet<String> linkArts;

    public HChargeTkCrawler() {
        linkHeroes = new HashSet<>();
        linkArts = new HashSet<>();
        listHeroesRemain = new ArrayBlockingQueue<>(1024);
        listArtsRemain = new ArrayBlockingQueue<>(1024);
    }

    public static void main(String[] args) throws InterruptedException {
        HChargeTkCrawler hChargeTkCrawler = new HChargeTkCrawler();
        ThreadGetListHeroes threadGetListHeroes = new ThreadGetListHeroes(hChargeTkCrawler.linkHeroes, hChargeTkCrawler.listHeroesRemain);
        ThreadGetListArts threadGetListArts = new ThreadGetListArts(hChargeTkCrawler.linkArts, hChargeTkCrawler.listHeroesRemain, hChargeTkCrawler.listArtsRemain);
        ThreadDownloadArts threadDownloadArts = new ThreadDownloadArts(hChargeTkCrawler.listArtsRemain);

        long start = System.currentTimeMillis();
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        List<Callable<Object>> tasks = new ArrayList<>();
        tasks.add(threadGetListHeroes);
        tasks.add(threadGetListArts);
        tasks.add(threadDownloadArts);
        List<Future<Object>> futures = executorService.invokeAll(tasks);
        long end = System.currentTimeMillis();
        System.out.println("take time: = " + (end - start));
    }
}

class ThreadGetListHeroes implements Runnable, Callable<Object> {
    private HashSet<String> linkHeroes;
    private BlockingQueue<String> listHeroesRemain;

    ThreadGetListHeroes(HashSet<String> linkHeroes, BlockingQueue<String> listHeroesRemain) {
        this.linkHeroes = linkHeroes;
        this.listHeroesRemain = listHeroesRemain;
    }

    @Override
    public void run() {
        try {
            Document document = Jsoup.connect("https://hcharge.tk/Heroes").get();
            //3. Parse the HTML to extract links to other URLs
            Elements linksOnPage = document.select("a[href]");
            //5. For each extracted URL... go back to Step 4.
            for (Element page : linksOnPage) {
                String urlHero = page.attr("abs:href");
                if (!linkHeroes.contains(urlHero)) {
                    if (urlHero.startsWith("https://hcharge.tk/Heroes/")) {
                        System.out.println("link heroes <<< " + urlHero);
                        linkHeroes.add(urlHero);
                        listHeroesRemain.add(urlHero);
                    }
                }
            }
            listHeroesRemain.add("END");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Object call() throws Exception {
        run();
        return null;
    }
}

class ThreadGetListArts implements Runnable, Callable<Object> {
    private HashSet<String> linkArts;
    private BlockingQueue<String> listHeroesRemain;
    private BlockingQueue<String> listArtsRemain;

    ThreadGetListArts(HashSet<String> linkArts, BlockingQueue<String> listHeroesRemain, BlockingQueue<String> listArtsRemain) {
        this.linkArts = linkArts;
        this.listHeroesRemain = listHeroesRemain;
        this.listArtsRemain = listArtsRemain;
    }

    @Override
    public void run() {
        boolean endSignal = false;
        while (!endSignal) {
            if (listHeroesRemain.size() != 0) {
                String linkHero = listHeroesRemain.remove();
                if (linkHero.equals("END")) {
                    endSignal = true;
                    listArtsRemain.add("END");
                } else {
                    try {
                        Document document = Jsoup.connect(linkHero).get();
                        Element heroArt = document.getElementById("heroArt");
                        String urlArt = "https://hcharge.tk/" + heroArt.attr("src");
                        if (!linkArts.contains(urlArt)) {
                            linkArts.add(urlArt);
                            listArtsRemain.add(urlArt);
                            System.out.println("link arts <<< " + urlArt);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public Object call() throws Exception {
        run();
        return null;
    }
}

class ThreadDownloadArts implements Runnable, Callable<Object> {
    private BlockingQueue<String> listArtsRemain;

    ThreadDownloadArts(BlockingQueue<String> listArtsRemain) {
        this.listArtsRemain = listArtsRemain;
    }

    @Override
    public void run() {
        boolean endSignal = false;
        while (!endSignal) {
            if (listArtsRemain.size() != 0) {
                String linkArt = listArtsRemain.remove();
                if (linkArt.equals("END")) {
                    endSignal = true;
                } else {
                    BufferedImage image = null;
                    try {
                        URL url = new URL(linkArt);
                        // read the url
                        image = ImageIO.read(url);

                        // for jpg
                        String[] names = linkArt.split("/");
                        String name = names[names.length - 1];
                        File f = new File("arts/" + name + ".jpg");
                        if (!f.exists()) {
                            ImageIO.write(image, "jpg", new File("arts/" + name + ".jpg"));
                            System.out.println("downloaded >>>" + linkArt);
                        }
                        listArtsRemain.remove(0);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public Object call() throws Exception {
        run();
        return null;
    }
}
