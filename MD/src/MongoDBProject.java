
// java io
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

// java.text package : provide class, interface for handling text, dates, numbers and message
import java.text.SimpleDateFormat;

// java until
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.Date;

// bson
import org.bson.Document;
import org.bson.conversions.Bson;
// json
import org.json.JSONObject;
import org.json.XML;

import com.mongodb.BasicDBObject;

import com.mongodb.DBCollection;
import com.mongodb.DBCursor;

import com.mongodb.MongoClient;

// mongodb client
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;

public class MongoDBProject {

	public static void main(String[] args) throws Exception {
		/**
		 * Convert the xml file to json file
		 */
		// result json file path
		String fileName = "D:\\temp.json";
		try {
			// 1. read xml file
			File file = new File("abcd.xml");
			// 1.1 get the stream from file
			InputStream inputStream = new FileInputStream(file);
			// 1.2 Store input stream in stringbuilder
			StringBuilder builder = new StringBuilder();
			int ptr = 0;
			while ((ptr = inputStream.read()) != -1) {
				builder.append((char) ptr);
			}

			String xml = builder.toString();
			// 2. convert the xml to json object
			JSONObject jsonObj = XML.toJSONObject(xml);
			// 3. write in json file
			FileWriter fileWriter = new FileWriter(fileName);
			// 3.1 wrap FileWriter in BufferedWriter
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
			// 3.2 use bufferedWriter to write jsonObj in json file
			for (int i = 0; i < jsonObj.toString().split(",").length; i++) {
				System.out.println(jsonObj.toString().split(",")[i]);
				bufferedWriter.write(jsonObj.toString().split(",")[i]);
				bufferedWriter.write("\n");
			}
			// 3.3 close the buffer writer
			bufferedWriter.close();
		} catch (IOException ex) {
			System.out.println("Error writing to file '" + fileName + "'");
		} catch (Exception e) {
			e.printStackTrace();
		}

		/**
		 * Print the all collection in db1 Reference: prof code
		 */
		// initialize the client object
		MongoClient mongoClient = new MongoClient();
		// get the 'test' dataset
		MongoDatabase dbObj = mongoClient.getDatabase("db1");

		// list its collections
		for (String name : dbObj.listCollectionNames()) {
			System.out.println("Collections inside this db:" + name);
		}

		/**
		 * Print the doc in collection "test7" in db1 Reference: prof code
		 */
		// Or explicitly we can go to its collection
		MongoCollection<Document> col = dbObj.getCollection("test7");

		// how to read the content of a document.
		Iterator it = col.find().iterator();
		while (it.hasNext()) {
			System.out.println("docs inside the col:" + it.next());
		}

		/**
		 * Insert the json file in mongodb
		 */
		try {
			// 1. connect to database at local host 27017
			MongoClient mk = new MongoClient("localhost", 27017);
			// 2. get the database name
			MongoDatabase md = mk.getDatabase("db1");
			// 3. create a collection in db1
			MongoCollection<Document> collection = md.getCollection("test7");
			// 4. read the json file
			BufferedReader br = new BufferedReader(new FileReader("temp.json"));
			String docs = null;
			// 5. insert the json data in db1
			while ((docs = br.readLine()) != null) {

				Document var = Document.parse(docs);
				// get the mdate field
				String string = (String) var.get("mdate");
				// Create a SimpleDateFormat formatter with the specific format
				SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
				// Use formatter to transfer the date
				Date date = formatter.parse(string);

				collection.insertOne(var.append("mdate_", date));

				System.out.println(date.getClass());// use getClass() to test the data type
			}
			System.out.println("Documents inserted successfully");
			// 6. close the mongo client
			mk.close();
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		/**
		 * Test the query
		 */
		MongoClient mk = new MongoClient("localhost", 27017);
		MongoDatabase md = mk.getDatabase("db1");
		MongoCollection<Document> collection = md.getCollection("test7");

		String patternStr = "Fast";
		Pattern pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
		Bson filter2 = Filters.regex("title", pattern);
		collection.find(filter2).forEach((Consumer<? super Document>) doc -> System.out.println(doc.toJson()));

		// Query: find the match text
		collection.createIndex(Indexes.text("title"));
		// 1. find the text "fast"
		Bson filter = Filters.text("Fast");
		// 2. print the doc that contain "fast"
		collection.find(filter).forEach((Consumer<? super Document>) doc -> System.out.println(doc.toJson()));

		// Query: find # of doc that title is "Fast 5"
		// 1. use aggregate to group the value
		List<String> l = new ArrayList<>();
		collection.aggregate(
				// 2. pass a list to perform aggregate stage
				Arrays.asList(
						// 3. find the "title" field is "Fast 5"
						Aggregates.match(Filters.regex("title", pattern)),
//		      Aggregates.match(Filters.eq("title", "Fast 5")),
						// 4. group the matching doc by "_id" field
						// accumulating doc for each distinct value "_id"
						Aggregates.group("$_id", Accumulators.sum("count", 1))))
				// 5. print the output
				.forEach((Consumer<? super Document>) doc -> System.out.println(doc.toJson()));

//		 Issue: Java variable type 
//		 Problem:
//		 1. insert "mdate: yyyy-MM-DD" as string in mogodb
//		 2. can not use string to find the date range because this "mdate" Date type is not date type, is string type
//		 Solve:
//		 1. insert "mdate: yyyy-MM-DD" as "date" data type in mongodb
//		 2. find the mdate(Date type) that is between xxxx and xxxx
//		 Notice:
//		 0. if we directly insert json file into mongodb
//		    we will only get integer and string type data
//		    so if we need the Date data type
//		    we first need to convert the string date
//		 1. 
//		 LocalDate(include time zone) is different from Date
//		 Mongodb can only use Date type
//		 2.
//		 If we want to insert the new document, first we need to drop the whole collection.
//		 Otherwise, we will get the duplicate key error.

		// Query : find the column that date range is between 2012.01.02 and 2015.02.20
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd");

		String date = "2012.01.02";
		String date1 = "2015.02.20";
		Date startDate = simpleDateFormat.parse(date);
		Date endDate = simpleDateFormat.parse(date1);
		BasicDBObject query = new BasicDBObject("mdate_", new BasicDBObject("$gte", startDate).append("$lt", endDate));

		DBCollection collection2 = mongoClient.getDB("db1").getCollection("test7");

		DBCursor cursor = collection2.find(query);

		try {
			while (cursor.hasNext()) {
				System.out.println(cursor.next());
			}
		} finally {
			cursor.close();
		}
	}

}