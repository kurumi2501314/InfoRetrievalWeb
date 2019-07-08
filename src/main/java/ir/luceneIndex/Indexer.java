package ir.luceneIndex;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.management.Query;

import org.apache.commons.io.FileUtils;
import org.apache.commons.csv.*;
import org.apache.commons.csv.CSVFormat.Predefined;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import com.huaban.analysis.jieba.JiebaSegmenter;

import ir.util.seg.SegmentAnalyzer;
import ir.util.seg.StopWordsLoader;
import ir.util.seg.jieba.JiebaAnalyzer;
import net.sourceforge.pinyin4j.PinyinHelper;

public class Indexer {
	// 停用词
	private final static CharArraySet STOP_WORDS = new CharArraySet(StopWordsLoader.stopWords, true);

	/**
	 * 根据输入的分词器分别使用不同写入索引文件的方法
	 * 
	 * @param analyzer   使用的分词器 提前进行设置
	 * @param index_path 存放输出索引文件的路径
	 * @param csv_path   存放输入csv数据文件的路径
	 */
	public static void luceneCreateIndex(Analyzer analyzer, String index_path, String csv_path) throws Exception {
		// 指定索引存放的位置
		Directory directory = FSDirectory.open(Paths.get(new File(index_path).getPath()));
		System.out.println("pathname" + Paths.get(new File(index_path).getPath()));
		// 创建indexwriterConfig(参数分词器)
		IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
		// 创建indexwriter对象(文件对象,索引配置对象)
		IndexWriter indexWriter = new IndexWriter(directory, indexWriterConfig);
		// 读csv
		String[] headers = new String[] { "id", "abstract", "address", "applicant", "application_date",
				"application_number", "application_publish_number", "classification_number", "filing_date",
				"grant_status", "inventor", "title", "year" };

		FileReader reader = new FileReader(csv_path);
		CSVParser parser = CSVFormat.DEFAULT.withHeader(headers).parse(reader);

		// 记录条目数量(控制测试条目)
		int i = 0;
		for (CSVRecord record : parser) {
			// 去掉第一行标题
			if (i == 0) {
				i++;
				continue;
			}
			else if (i%10000 == 0) {
				System.out.println(i);
			}
			i++;
			//System.out.println("第" + i + "项:");
			//System.out.println("title:" + record.get("title"));
			// preserved_type保护域不分词
			// default_type默认分词
			// 分词模式DOCS
			FieldType preserved_type = new FieldType();
			preserved_type.setTokenized(false);
			preserved_type.setStored(true);

			preserved_type.setIndexOptions(IndexOptions.DOCS);
			FieldType default_type = new FieldType();
			default_type.setTokenized(true);
			default_type.setStored(true);
//			default_type.setStoreTermVectors(true);
//			default_type.setStoreTermVectorPositions(true);
//			default_type.setStoreTermVectorOffsets(true);
			default_type.setIndexOptions(IndexOptions.DOCS);

			// 创建文件域名
			// Field(域的名称,域的内容,fieldtype)
			Field id_field = new Field("id", record.get("id"), preserved_type);
			Field abstract_field = new Field("abstract", record.get("abstract"), default_type);
			Field address_field = new Field("address", record.get("address"), default_type);
			Field applicant_field = new Field("applicant", record.get("applicant"), preserved_type);

			Field application_date_field = new Field("application_date", record.get("application_date"),
					preserved_type);
			Field application_number_field = new Field("application_number", record.get("application_number"),
					preserved_type);
			Field application_publish_number_field = new Field("application_publish_number",
					record.get("application_publish_number"), preserved_type);

			Field classification_number_field = new Field("classification_number", record.get("classification_number"),
					preserved_type);
			// System.out.println(record.get("classification_number"));
			Field filing_date_field = new Field("filing_date", record.get("filing_date"), preserved_type);

			Field grant_status_field = new Field("grant_status", record.get("grant_status"), preserved_type);

			// Field inventor_field = new Field("inventor", record.get("inventor"),
			// preserved_type);
			Field title_field = new Field("title", record.get("title"), default_type);
			Field year_field = new Field("year", record.get("year"), preserved_type);

			Document indexableFields = new Document();
			indexableFields.add(id_field);
			indexableFields.add(abstract_field);
			indexableFields.add(address_field);
			indexableFields.add(applicant_field);

			indexableFields.add(application_date_field);
			indexableFields.add(application_number_field);
			indexableFields.add(application_publish_number_field);

			indexableFields.add(classification_number_field);
			indexableFields.add(filing_date_field);
			indexableFields.add(grant_status_field);

			// indexableFields.add(inventor_field);
			indexableFields.add(title_field);
			indexableFields.add(year_field);

			// ***新加的numericDocValue域
			// 存储date的数据类型为long并且存储在新的numericDocValue域中
			String date = record.get("application_date").replace(".", "");
			long d = Long.valueOf(date).longValue();
			Field application_date_long_field = new NumericDocValuesField("application_date_long", d);
			// 存储status的数据类型为long并且存储在新的numericDocValue域中
			long status = Long.valueOf(record.get("grant_status")).longValue();
			Field grant_status_long_field = new NumericDocValuesField("grant_status_long", status);
			// 加入doc
			indexableFields.add(application_date_long_field);
			indexableFields.add(grant_status_long_field);

			// 添加姓名域以及首字母域
			// 姓名域改为多值域
			String inventorS = record.get("inventor");
			String inventor[] = inventorS.split(";");
			for (String out : inventor) {
				String firstW = "";
				// System.out.println(out);
				if (out.charAt(0) >= 'A' && out.charAt(0) <= 'Z' || out.charAt(0) >= 'a' && out.charAt(0) <= 'z') {
					firstW = String.valueOf(out.charAt(0));
					// System.out.println(firstW);
				} else {
					String temp[] = PinyinHelper.toHanyuPinyinStringArray(out.charAt(0));
					if (temp != null)
						firstW = String.valueOf(temp[0].charAt(0));
					// System.out.println(firstW);
				}
				firstW = firstW.toUpperCase();
				// 姓名域
				Field inventor_field = new Field("inventor", out, preserved_type);
				// 首字母域FirstWorld
				Field inventor_firstW_field = new Field("inventor_firstW", firstW, preserved_type);

				indexableFields.add(inventor_field);
				indexableFields.add(inventor_firstW_field);
			}

			// 添加分类域 为多值域
			String class_num = record.get("classification_number");
			String classification[] = class_num.split(";|//|\\(|,");
			for (String out : classification) {

				if (!out.equals("") && out.charAt(0) >= 'A' && out.charAt(0) <= 'Z') {
					Field class_field = new Field("class", out.substring(0, 1), preserved_type);
					indexableFields.add(class_field);
					//System.out.print(out);
				}
			}
			//System.out.println();

			// 创建索引 并写入索引库
			indexWriter.addDocument(indexableFields);
			// indexWriter.forceMerge(1);
		}
		reader.close();
		// 关闭indexWriter
		indexWriter.close();
	}

	/**
	 * function1 调用luceneCreatIndex实现jieba index模式分词
	 */
	public static void create_jiebaIndex_index() throws Exception {
		// 指定索引存放的位置
		String path = "D:\\Lucene_Path\\Lucene_index\\fuzzy_index";
		String csv_path = "D:\\Lucene_Path\\newpatent.csv";
		// 创建一个分词器 jieba index模式 加入停用词
		JiebaAnalyzer jiebaAnalyzer = new JiebaAnalyzer(JiebaSegmenter.SegMode.INDEX, STOP_WORDS);
		luceneCreateIndex(jiebaAnalyzer, path, csv_path);
	}

	/**
	 * function2 调用luceneCreatIndex实现jieba search模式分词
	 */
	public static void create_jiebaSearch_index() throws Exception {
		// 指定索引存放的位置
		String path = "D:\\Lucene_Path\\Lucene_index\\accurate_index";
		String csv_path = "D:\\Lucene_Path\\newpatent.csv";
		// 创建一个分词器 jieba search模式 加入停用词
		JiebaAnalyzer jiebaAnalyzer = new JiebaAnalyzer(JiebaSegmenter.SegMode.SEARCH, STOP_WORDS);
		luceneCreateIndex(jiebaAnalyzer, path, csv_path);
	}

	/**
	 * function3 调用luceneCreatIndex 使用StandardAnalyzer实现单字切分
	 */
	public static void create_singleWord_index() throws Exception {
		// 指定索引存放的位置
		String path = "D:\\Lucene_Path\\Lucene_index\\single_word_index";
		String csv_path = "D:\\Lucene_Path\\newpatent.csv";
		StandardAnalyzer analyzer = new StandardAnalyzer();
		luceneCreateIndex(analyzer, path, csv_path);
	}

	/**
	 * function4 调用luceneCreatIndex 使用CJKAnalyzer实现双字切分
	 */
	public static void create_doubleWord_index() throws Exception {
		// 指定索引存放的位置
		String path = "D:\\Lucene_Path\\Lucene_index\\double_word_index";
		String csv_path = "D:\\Lucene_Path\\newpatent.csv";
		// 创建一个分词器
		CJKAnalyzer cjkAnalyzer = new CJKAnalyzer();
		luceneCreateIndex(cjkAnalyzer, path, csv_path);
	}

	/**
	 * old写入索引文件 jieba分词的index模式
	 * 
	 * @author WC
	 */
	public static void luceneCreateIndex_jiebaindex() throws Exception {
		// 指定索引存放的位置
		Directory directory = FSDirectory
				.open(Paths.get(new File("D:\\Lucene_Path\\Lucene_index\\fuzzy_index").getPath()));
		System.out.println("pathname" + Paths.get(new File("D:\\Lucene_Path\\Lucene_index\\fuzzy_index").getPath()));
		// 创建一个分词器 jieba index模式 加入停用词
		JiebaAnalyzer jiebaAnalyzer = new JiebaAnalyzer(JiebaSegmenter.SegMode.INDEX, STOP_WORDS);
		// 创建indexwriterConfig(参数分词器)
		IndexWriterConfig indexWriterConfig = new IndexWriterConfig(jiebaAnalyzer);
		// 创建indexwriter对象(文件对象,索引配置对象)
		IndexWriter indexWriter = new IndexWriter(directory, indexWriterConfig);
		// 读csv
		String[] headers = new String[] { "id", "abstract", "address", "applicant", "application_date",
				"application_number", "application_publish_number", "classification_number", "filing_date",
				"grant_status", "inventor", "title", "year" };
		String csvPath = "D:\\Lucene_Path\\newpatent.csv";

		FileReader reader = new FileReader(csvPath);
		CSVParser parser = CSVFormat.DEFAULT.withHeader(headers).parse(reader);

		// 记录条目数量(控制测试条目)
		int i = 0;
		for (CSVRecord record : parser) {
			// 去掉第一行标题
			if (i == 0) {
				i++;
				continue;
			} else if (i == 1000) {
				break;
			}
			i++;
			// System.out.print("第"+i+"项:");
			System.out.println(record.get("title"));
			// preserved_type保护域不分词
			// default_type默认分词
			// 分词模式DOCS
			FieldType preserved_type = new FieldType();
			preserved_type.setTokenized(false);
			preserved_type.setStored(true);

			preserved_type.setIndexOptions(IndexOptions.DOCS);
			FieldType default_type = new FieldType();
			default_type.setTokenized(true);
			default_type.setStored(true);
			default_type.setStoreTermVectors(true);
			default_type.setStoreTermVectorPositions(true);
			default_type.setStoreTermVectorOffsets(true);
			default_type.setIndexOptions(IndexOptions.DOCS);

			// 创建文件域名
			// Field(域的名称,域的内容,fieldtype)
			Field id_field = new Field("id", record.get("id"), preserved_type);
			Field abstract_field = new Field("abstract", record.get("abstract"), default_type);
			Field address_field = new Field("address", record.get("address"), default_type);
			Field applicant_field = new Field("applicant", record.get("applicant"), preserved_type);

			Field application_date_field = new Field("application_date", record.get("application_date"),
					preserved_type);
			Field application_number_field = new Field("application_number", record.get("application_number"),
					preserved_type);
			Field application_publish_number_field = new Field("application_publish_number",
					record.get("application_publish_number"), preserved_type);

			Field classification_number_field = new Field("classification_number", record.get("classification_number"),
					preserved_type);
			System.out.println(record.get("classification_number"));
			Field filing_date_field = new Field("filing_date", record.get("filing_date"), preserved_type);

			Field grant_status_field = new Field("grant_status", record.get("grant_status"), preserved_type);

			// Field inventor_field = new Field("inventor", record.get("inventor"),
			// preserved_type);
			Field title_field = new Field("title", record.get("title"), default_type);
			Field year_field = new Field("year", record.get("year"), preserved_type);

			Document indexableFields = new Document();
			indexableFields.add(id_field);
			indexableFields.add(abstract_field);
			indexableFields.add(address_field);
			indexableFields.add(applicant_field);

			indexableFields.add(application_date_field);
			indexableFields.add(application_number_field);
			indexableFields.add(application_publish_number_field);

			indexableFields.add(classification_number_field);
			indexableFields.add(filing_date_field);
			indexableFields.add(grant_status_field);

			// indexableFields.add(inventor_field);
			indexableFields.add(title_field);
			indexableFields.add(year_field);

			// ***新加的numericDocValue域
			// 存储date的数据类型为long并且存储在新的numericDocValue域中
			String date = record.get("application_date").replace(".", "");
			long d = Long.valueOf(date).longValue();
			Field application_date_long_field = new NumericDocValuesField("application_date_long", d);
			// 存储status的数据类型为long并且存储在新的numericDocValue域中
			long status = Long.valueOf(record.get("grant_status")).longValue();
			Field grant_status_long_field = new NumericDocValuesField("grant_status_long", status);
			// 加入doc
			indexableFields.add(application_date_long_field);
			indexableFields.add(grant_status_long_field);

			// 添加姓名域以及首字母域
			// 姓名域改为多值域
			String inventorS = record.get("inventor");
			String inventor[] = inventorS.split(";");
			for (String out : inventor) {
				String firstW = "";
				// System.out.println(out);
				if (out.charAt(0) >= 'A' && out.charAt(0) <= 'Z' || out.charAt(0) >= 'a' && out.charAt(0) <= 'z') {
					firstW = String.valueOf(out.charAt(0));
					// System.out.println(firstW);
				} else {
					String temp[] = PinyinHelper.toHanyuPinyinStringArray(out.charAt(0));
					if (temp != null)
						firstW = String.valueOf(temp[0].charAt(0));
					// System.out.println(firstW);
				}
				firstW = firstW.toUpperCase();
				// 姓名域
				Field inventor_field = new Field("inventor", out, preserved_type);
				// 首字母域FirstWorld
				Field inventor_firstW_field = new Field("inventor_firstW", firstW, preserved_type);

				indexableFields.add(inventor_field);
				indexableFields.add(inventor_firstW_field);
			}

			// 创建索引 并写入索引库
			indexWriter.addDocument(indexableFields);
			// indexWriter.forceMerge(1);
		}
		reader.close();
		// 关闭indexWriter
		indexWriter.close();
	}

	/**
	 * old建立新索引文件 jieba分词的search模式
	 * 
	 * @author WC
	 */
	public static void luceneCreateIndex_jiebasearch() throws Exception {
		// 指定新索引存放的位置
		Directory directory = FSDirectory
				.open(Paths.get(new File("D:\\Lucene_Path\\Lucene_index\\accurate_index").getPath()));
		System.out.println("pathname" + Paths.get(new File("D:\\Lucene_Path\\Lucene_index\\accurate_index").getPath()));
		// 创建一个分词器 jieba search模式 加入停用词
		JiebaAnalyzer jiebaAnalyzer = new JiebaAnalyzer(JiebaSegmenter.SegMode.SEARCH, STOP_WORDS);
		// 创建indexwriterConfig(参数分词器)
		IndexWriterConfig indexWriterConfig = new IndexWriterConfig(jiebaAnalyzer);
		// 创建indexwriter对象(文件对象,索引配置对象)
		IndexWriter indexWriter = new IndexWriter(directory, indexWriterConfig);
		// 读csv
		String[] headers = new String[] { "id", "abstract", "address", "applicant", "application_date",
				"application_number", "application_publish_number", "classification_number", "filing_date",
				"grant_status", "inventor", "title", "year" };
		String csvPath = "D:\\Lucene_Path\\newpatent.csv";

		FileReader reader = new FileReader(csvPath);
		CSVParser parser = CSVFormat.DEFAULT.withHeader(headers).parse(reader);

		// 记录条目数量(控制测试条目)
		int i = 0;
		for (CSVRecord record : parser) {
			// 去掉第一行标题
			if (i == 0) {
				i++;
				continue;
			}
			i++;
			System.out.print("第" + i + "项:");
			System.out.println(record.get("title"));
			// preserved_type保护域不分词
			// default_type默认分词
			// 分词模式DOCS
			FieldType preserved_type = new FieldType();
			preserved_type.setTokenized(false);
			preserved_type.setStored(true);
			preserved_type.setIndexOptions(IndexOptions.DOCS);
			FieldType default_type = new FieldType();
			default_type.setTokenized(true);
			default_type.setStored(true);
			default_type.setStoreTermVectors(true);
			default_type.setStoreTermVectorPositions(true);
			default_type.setStoreTermVectorOffsets(true);
			default_type.setIndexOptions(IndexOptions.DOCS);

			// 创建文件域名
			// Field(域的名称,域的内容,fieldtype)
			Field id_field = new Field("id", record.get("id"), preserved_type);
			Field abstract_field = new Field("abstract", record.get("abstract"), default_type);
			Field address_field = new Field("address", record.get("address"), default_type);
			Field applicant_field = new Field("applicant", record.get("applicant"), preserved_type);

			Field application_date_field = new Field("application_date", record.get("application_date"),
					preserved_type);
			Field application_number_field = new Field("application_number", record.get("application_number"),
					preserved_type);
			Field application_publish_number_field = new Field("application_publish_number",
					record.get("application_publish_number"), preserved_type);

			Field classification_number_field = new Field("classification_number", record.get("classification_number"),
					preserved_type);
			Field filing_date_field = new Field("filing_date", record.get("filing_date"), preserved_type);
			Field grant_status_field = new Field("grant_status", record.get("grant_status"), preserved_type);

			// Field inventor_field = new Field("inventor", record.get("inventor"),
			// preserved_type);
			Field title_field = new Field("title", record.get("title"), default_type);
			Field year_field = new Field("year", record.get("year"), preserved_type);

			Document indexableFields = new Document();
			indexableFields.add(id_field);
			indexableFields.add(abstract_field);
			indexableFields.add(address_field);
			indexableFields.add(applicant_field);

			indexableFields.add(application_date_field);
			indexableFields.add(application_number_field);
			indexableFields.add(application_publish_number_field);

			indexableFields.add(classification_number_field);
			indexableFields.add(filing_date_field);
			indexableFields.add(grant_status_field);

			// indexableFields.add(inventor_field);
			indexableFields.add(title_field);
			indexableFields.add(year_field);

			// ***新加的numericDocValue域
			// 存储date的数据类型为long并且存储在新的numericDocValue域中
			String date = record.get("application_date").replace(".", "");
			long d = Long.valueOf(date).longValue();
			Field application_date_long_field = new NumericDocValuesField("application_date_long", d);
			// 存储status的数据类型为long并且存储在新的numericDocValue域中
			long status = Long.valueOf(record.get("grant_status")).longValue();
			Field grant_status_long_field = new NumericDocValuesField("grant_status_long", status);
			// 加入doc
			indexableFields.add(application_date_long_field);
			indexableFields.add(grant_status_long_field);

			// 添加姓名域以及首字母域
			// 姓名域改为多值域
			String inventorS = record.get("inventor");
			String inventor[] = inventorS.split(";");
			for (String out : inventor) {
				String firstW = "";
				// System.out.println(out);
				if (out.charAt(0) >= 'A' && out.charAt(0) <= 'Z' || out.charAt(0) >= 'a' && out.charAt(0) <= 'z') {
					firstW = String.valueOf(out.charAt(0));
					// System.out.println(firstW);
				} else {
					String temp[] = PinyinHelper.toHanyuPinyinStringArray(out.charAt(0));
					if (temp != null)
						firstW = String.valueOf(temp[0].charAt(0));
					// System.out.println(firstW);
				}
				firstW = firstW.toUpperCase();
				// 姓名域
				Field inventor_field = new Field("inventor", out, preserved_type);
				// 首字母域FirstWorld
				Field inventor_firstW_field = new Field("inventor_firstW", firstW, preserved_type);

				indexableFields.add(inventor_field);
				indexableFields.add(inventor_firstW_field);
			}

			// indexableFields.add(fileSizeField);
			// 创建索引 并写入索引库
			indexWriter.addDocument(indexableFields);
			// indexWriter.forceMerge(1);
		}
		reader.close();
		// 关闭indexWriter
		indexWriter.close();
		System.out.println("一共有" + i + "条数据");
	}

	// 多条件查询
	public static void boolean_search(String content) throws IOException {
		// 指定索引库存放路径
		// E:\Lucene_Path\Lucene_index
		Directory directory = FSDirectory.open(Paths.get(new File("D:\\Lucene_Path\\Lucene_index").getPath()));
		// 创建indexReader对象
		IndexReader indexReader = DirectoryReader.open(directory);
		// 创建indexSearcher对象
		IndexSearcher indexSearcher = new IndexSearcher(indexReader);

		/** 创建多条件查询 **/
		// BooleanQuery query = new BooleanQuery();
		// String searchField="INVENTOR";
		// String keyword=content;
		TermQuery query1 = new TermQuery(new Term("abstract", content));
		TermQuery query2 = new TermQuery(new Term("address", content));
		TermQuery query3 = new TermQuery(new Term("grant_status", content));
		TermQuery query4 = new TermQuery(new Term("inventor", content));
		TermQuery query5 = new TermQuery(new Term("title", content));
		TermQuery query6 = new TermQuery(new Term("year", content));
		BooleanQuery.Builder builder = new BooleanQuery.Builder();
		// 1．MUST和MUST：取得连个查询子句的交集。
		// 2．MUST和MUST_NOT：表示查询结果中不能包含MUST_NOT所对应得查询子句的检索结果。
		// 3．SHOULD与MUST_NOT：连用时，功能同MUST和MUST_NOT。
		// 4．SHOULD与MUST连用时，结果为MUST子句的检索结果,但是SHOULD可影响排序。
		// 5．SHOULD与SHOULD：表示“或”关系，最终检索结果为所有检索子句的并集。
		// 6．MUST_NOT和MUST_NOT：无意义，检索无结果。
		builder.add(query1, Occur.SHOULD);
		builder.add(query2, Occur.SHOULD);
		builder.add(query3, Occur.SHOULD);
		builder.add(query4, Occur.SHOULD);
		builder.add(query5, Occur.SHOULD);
		builder.add(query6, Occur.SHOULD);
		BooleanQuery booleanQuery = builder.build();

		// 普通查询 域名 查询内容
		// TermQuery query = new TermQuery(new Term("abstract", content));

		// 执行查询
		TopDocs topDocs = indexSearcher.search(booleanQuery, 10);
		System.out.println("查询结果的总数" + topDocs.totalHits);
		// 遍历查询结果
		for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
			// scoreDoc.doc 属性就是doucumnet对象的id
			Document doc = indexSearcher.doc(scoreDoc.doc);
			System.out.println(doc.getField("id"));
			System.out.println(doc.getField("abstract"));
			System.out.println(doc.getField("inventor"));
			// System.out.println(doc.getField("ABSTRACT"));
			// System.out.println(doc.getField("INVENTOR"));
			/*
			 * System.out.println(doc.getField("fileName"));
			 * System.out.println(doc.getField("fileContent"));
			 * System.out.println(doc.getField("filePath"));
			 * System.out.println(doc.getField("fileSize"));
			 */
		}
		indexReader.close();
	}

	// 单域多条件查询
	public static void multi_search(String field, String[] content, int dir) throws IOException {
		// 指定索引库存放路径
		// D:\Lucene_Path\Lucene_index
		Directory directory = FSDirectory
				.open(Paths.get(new File("D:\\Lucene_Path\\Lucene_index\\fuzzy_index").getPath()));
		if (dir == 1) {
			directory = FSDirectory
					.open(Paths.get(new File("D:\\Lucene_Path\\Lucene_index\\accurate_index").getPath()));
		} else if (dir == 2) {
			directory = FSDirectory
					.open(Paths.get(new File("D:\\Lucene_Path\\Lucene_index\\single_word_index").getPath()));
		} else if (dir == 3) {
			directory = FSDirectory
					.open(Paths.get(new File("D:\\Lucene_Path\\Lucene_index\\double_word_index").getPath()));
		} else if (dir == 4) {
			directory = FSDirectory.open(Paths.get(new File("D:\\Lucene_Path\\Lucene_index\\indexe0").getPath()));
		} else if (dir == 5) {
			directory = FSDirectory.open(Paths.get(new File("D:\\Lucene_Path\\Lucene_index\\indexe2").getPath()));
		}

		// 创建indexReader对象
		IndexReader indexReader = DirectoryReader.open(directory);
		// 创建indexSearcher对象
		IndexSearcher indexSearcher = new IndexSearcher(indexReader);

		/** 创建多关键词查询 **/
		BooleanQuery.Builder builder = new BooleanQuery.Builder();

		for (String text : content) {
			TermQuery query = new TermQuery(new Term(field, text));
			// 1．MUST和MUST：取得连个查询子句的交集。
			// 2．MUST和MUST_NOT：表示查询结果中不能包含MUST_NOT所对应得查询子句的检索结果。
			// 3．SHOULD与MUST_NOT：连用时，功能同MUST和MUST_NOT。
			// 4．SHOULD与MUST连用时，结果为MUST子句的检索结果,但是SHOULD可影响排序。
			// 5．SHOULD与SHOULD：表示“或”关系，最终检索结果为所有检索子句的并集。
			// 6．MUST_NOT和MUST_NOT：无意义，检索无结果。
			builder.add(query, Occur.MUST);
		}

		BooleanQuery booleanQuery = builder.build();

		// 普通查询 域名 查询内容
		// TermQuery query = new TermQuery(new Term("abstract", content));

		// 执行查询
		TopDocs topDocs = indexSearcher.search(booleanQuery, 20);
		System.out.println("查询结果的总数" + topDocs.totalHits);
		// 遍历查询结果
		for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
			// scoreDoc.doc 属性就是doucumnet对象的id
			Document doc = indexSearcher.doc(scoreDoc.doc);
			// System.out.println(doc.getValues("id")[0]);
			// System.out.println(doc.getValues("title")[0]);
			System.out.println(doc.getValues("abstract")[0]);
		}
		indexReader.close();
	}

	public static void dif_search(String field, String[] content, int dir1, int dir2) {

	}

	public static void single_search(String field, String content) throws IOException {
		// 制定索引库存放路径
		Directory directory = FSDirectory
				.open(Paths.get(new File("D:\\Lucene_Path\\Lucene_index\\fuzzy_index").getPath()));
		// 创建indexReader对象
		IndexReader indexReader = DirectoryReader.open(directory);
		// 创建indexSearcher对象
		IndexSearcher indexSearcher = new IndexSearcher(indexReader);

		// 普通查询 域名 查询内容
		TermQuery query = new TermQuery(new Term(field, content));
		// 执行查询
		TopDocs topDocs = indexSearcher.search(query, 10);
		System.out.println("查询结果的总数" + topDocs.totalHits);
		// 遍历查询结果
		for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
			// scoreDoc.doc 属性就是doucumnet对象的id
			Document doc = indexSearcher.doc(scoreDoc.doc);
			System.out.println(doc.getValues("id")[0]);
			System.out.println(doc.getValues("title")[0]);
			System.out.println(doc.getValues("abstract")[0]);
			/** 姓名域变为多值域 输出方法变更 **/
			String inventorS[] = doc.getValues("class");
			for (String name : inventorS)
				System.out.println(name);

		}
		indexReader.close();
	}

	public static void main(String[] args) throws Exception {
		create_jiebaIndex_index();
		System.out.println("fuzzy_index创建完成");
		create_jiebaSearch_index();
		System.out.println("accruate_index创建完成");
		create_singleWord_index();
		System.out.println("single_word_index创建完成");
		create_doubleWord_index();
		System.out.println("double_word_index创建完成");
		// luceneCreateIndex_jiebaindex();
		// luceneCreateIndex_jiebasearch();
//		 String[] content = { "G","H"};
//		 multi_search("class", content, 0);

		// single_search("class","G");
	}
}
