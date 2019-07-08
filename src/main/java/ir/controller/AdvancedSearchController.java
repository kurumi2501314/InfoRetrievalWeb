package ir.controller;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.IndexSearcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import ir.enumDefine.BoolOptionSymbol;
import ir.enumDefine.FieldType;
import ir.enumDefine.SearchAccuracy;
import ir.luceneIndex.LuceneSearcher;
import ir.models.BoolExpression;
import ir.models.PatentsForView;
import ir.services.AdvancedSearchService;
import ir.util.seg.SegmentAnalyzer;

@Controller
public class AdvancedSearchController {
	
	@Autowired
	private AdvancedSearchService advancedSearchService;

	@GetMapping("/advancedSearch")//即什么样的页面会映射到这个方法上
	@ResponseBody
	public ModelAndView advancedSearch(@RequestParam(value="expressions",required=true)BoolExpression[] expressions,
			@RequestParam int page,
			@RequestParam(value="time_from",defaultValue="NO_LIMIT",required=false) String timeFrom,//起始时间
			@RequestParam(value="time_to",defaultValue="NO_LIMIT",required=false) String timeTo) {
		
		if(page<=0)
			page=1;
		try {
			timeFrom=timeFrom.replace('-', '.');
			timeTo=timeTo.replace('-', '.');
		}catch (Exception e) {
			// TODO: to error page
			e.printStackTrace();
			return null;
		}
		FieldType field;
		BoolOptionSymbol symbol;
		for(int i=0;i<expressions.length;i++) {
			try {
				field=FieldType.valueOf(expressions[i].field);
				symbol=BoolOptionSymbol.valueOf(expressions[i].symbol);
			}catch (Exception e) {
				// TODO: to error page
				return null;
			}
			
		}
		
		IndexSearcher luceneIndex = LuceneSearcher.indexes.get(SearchAccuracy.FUZZY);
		if(luceneIndex==null) {
			// TODO: to error page
			return null;
		}
		Analyzer analyzer = SegmentAnalyzer.getAnalyzer(SearchAccuracy.FUZZY);
		
		PatentsForView result;
		try {
			long startTime=System.currentTimeMillis();
			result=advancedSearchService.search(expressions,page,timeFrom,timeTo, luceneIndex, analyzer);
			System.out.println("搜索总共花费时间"+(System.currentTimeMillis()-startTime)+"ms");
		}catch (Exception e) {
			// TODO: handle exception
			return null;
		}
		
		ModelAndView modelAndView=new ModelAndView();
		modelAndView.setViewName("show2");//设置模板名称
		modelAndView.addObject("patentsForView", result);//加入返回的结果
		modelAndView.addObject("page", page);
		modelAndView.addObject("time_from", timeFrom);
		modelAndView.addObject("time_to", timeTo);
		modelAndView.addObject("number",result.getHitsNum());
		
		return null;
	}
}
