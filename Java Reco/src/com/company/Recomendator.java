package com.company;

import net.librec.common.LibrecException;
import net.librec.conf.Configuration;
import net.librec.data.model.TextDataModel;
import net.librec.eval.RecommenderEvaluator;
import net.librec.eval.rating.RMSEEvaluator;
import net.librec.filter.GenericRecommendedFilter;
import net.librec.recommender.Recommender;
import net.librec.recommender.RecommenderContext;
import net.librec.recommender.cf.ItemKNNRecommender;
import net.librec.recommender.item.RecommendedItem;
import net.librec.similarity.PCCSimilarity;
import net.librec.similarity.RecommenderSimilarity;

import java.io.FileInputStream;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by Colo on 17/11/2017.
 */
public class Recomendator {
    private static Recomendator ourInstance = new Recomendator();

    public static Recomendator getInstance() {
        return ourInstance;
    }

    private Recomendator() {
    }
    public List<RecommendedItem> recomend(String Usuario) throws IOException, LibrecException {
        List<RecommendedItem> recommendedItemList=reco(Usuario);
        int cont=0;
        while(cont<10) {
            cont++;
            recommendedItemList = reco(Usuario);
            if(!recommendedItemList.isEmpty()) {
                return recommendedItemList;
            }
        }
        if(recommendedItemList.isEmpty()){
            generateOutput("Error, El usuario no tiene perfil de recomendaciones");
            System.out.printf("El usuario no tiene perfil de recomendaciones");
            System.exit(0);
        }else{
            return recommendedItemList;
        }
        return recommendedItemList;
    }

    public List<RecommendedItem> reco(String Usuario) throws IOException, LibrecException {
        Configuration conf = new Configuration();
        TextDataModel dataModel=modelarDatos(conf);
        // Recomendador
        RecommenderContext context = new RecommenderContext(conf, dataModel);
        // Generar similaridad
        RecommenderSimilarity similarity=generateSimilarity(conf, dataModel);
        context.setSimilarity(similarity);
        // Crear recomenadador
        Recommender recommender = createRecomendator(conf,context);
        // Correr algoritmo
        recommender.recommend(context);
        // Ver que tan bueno es el resultado
        evaluateResult(recommender);
        // Hacer la lista para filtrar
        List<String> userIdList = new ArrayList<>();
        List<String> itemIdList = new ArrayList<>();
        //userIdList.add("1");
        //itemIdList.add("70");
        // Filtrar por los mejores
        List<RecommendedItem> recommendedItemList = recommender.getRecommendedList();
        GenericRecommendedFilter filter = new GenericRecommendedFilter();
        filter.setUserIdList(userIdList);
        filter.setItemIdList(itemIdList);
        //recommendedItemList = filter.filter(recommendedItemList);
        recommendedItemList.sort(Comparator.comparing(RecommendedItem::getValue));
        //Collections.reverse(recommendedItemList);

        FiltroRecos filtrar=new FiltroRecos(Usuario,recommendedItemList);
        List<RecommendedItem> filtrado=filtrar.filtrar();
        if (!userIsInList(recommendedItemList,Usuario)){
            return new LinkedList<RecommendedItem>();
        }
        if (filtrado.isEmpty()) {
            if(recommendedItemList.size()<5){
                generateOutput(recommendedItemList);
                return recommendedItemList;
            }else{
                generateOutput(recommendedItemList.subList(recommendedItemList.size() - 4, recommendedItemList.size() - 1));
                return recommendedItemList.subList(recommendedItemList.size() - 4, recommendedItemList.size() - 1);
            }
        }
        if(filtrado.size()<5){
            generateOutput(filtrado);
            return filtrado;
        }else{
            generateOutput(filtrado.subList(filtrado.size()-4,filtrado.size()-1));
            return filtrado.subList(filtrado.size() - 4, filtrado.size() - 1);
        }
    }

    private void generateOutput(String string) throws IOException {
        NewWriter writer=new NewWriter(string);
        writer.writeTextFile();
    }


    private boolean userIsInList(List<RecommendedItem> recommendedItemList, String usuario) {
        for (RecommendedItem recommendedItem : recommendedItemList) {
            if (Objects.equals(recommendedItem.getUserId(),usuario)){
                return true;
            }
        }
        return false;
    }

    private void generateOutput(List<RecommendedItem> recommendedItemList) throws IOException {
        NewWriter writer=new NewWriter(recommendedItemList);
        writer.writeFile();
    }

    private void evaluateResult(Recommender recommender) throws LibrecException {
        RecommenderEvaluator evaluator = new RMSEEvaluator();
        //System.out.println("RMSE:" + recommender.evaluate(evaluator));
    }

    private Recommender createRecomendator(Configuration conf, RecommenderContext context) {
        conf.set("rec.neighbors.knn.number", "5");
        Recommender recommender = new ItemKNNRecommender();
        recommender.setContext(context);
        return recommender;
    }

    private RecommenderSimilarity generateSimilarity(Configuration conf, TextDataModel dataModel) {
        conf.set("rec.recommender.similarity.key" ,"item");
        RecommenderSimilarity similarity = new PCCSimilarity();
        similarity.buildSimilarityMatrix(dataModel);
        return similarity;
    }

    private TextDataModel modelarDatos(Configuration conf) throws LibrecException, IOException {

        String confFilePath = "conf\\librec.properties";
        Properties prop = new Properties();
        prop.load(new FileInputStream(confFilePath));

        for(String name : prop.stringPropertyNames()) {
            conf.set(name, prop.getProperty(name));
        }

        TextDataModel dataModel = new TextDataModel(conf);
        dataModel.buildDataModel();
        return dataModel;
    }
}