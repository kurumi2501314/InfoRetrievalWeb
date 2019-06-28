package ir.services;

import org.springframework.stereotype.Service;

import ir.enumDefine.FirstLetterOfNamePinyin;
import ir.enumDefine.IsGranted;
import ir.enumDefine.SortedType;
import ir.models.PatentsForView;

@Service
public interface SearchService {

	public PatentsForView search(String field ,String keyWords,int page,FirstLetterOfNamePinyin letter,String timeFrom,//PatentsForView
			String timeTo,IsGranted isGranted,SortedType sortedType);
	
}
