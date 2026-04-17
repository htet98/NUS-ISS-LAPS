package nus_iss.LAPS.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import nus_iss.LAPS.repository.PublicHolidayRepository;
import nus_iss.LAPS.model.PublicHoliday;

@Service
public class PublicHolidayService 
{

    @Autowired
    private PublicHolidayRepository holidayRepository;

    public List<PublicHoliday> getAllHolidays() 
    {
        return holidayRepository.findAll();
    }
    
    public void saveHoliday(PublicHoliday holiday) 
    {
        holidayRepository.save(holiday);
    }
    
    public PublicHoliday getHolidayById(Long id) 
    {
        return holidayRepository.findById(id).orElse(null);
    }
    
    public void deleteHoliday(Long id) 
    {
        holidayRepository.deleteById(id);
    }
    
}