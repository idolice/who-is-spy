package com.spy.controller;

import com.spy.model.Gamer;
import com.spy.model.Room;
import com.spy.model.dao.GamerDao;
import com.spy.model.dao.RoomDao;
import com.spy.model.dao.VoteDao;
import com.spy.service.GameService;
import com.spy.service.VoteService;
import javassist.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;

@Controller
public class GamerController {

    GamerDao gamerDao;
    RoomDao roomDao;
    VoteDao voteDao;
    GameService gameService;
    VoteService voteService;

    @Autowired
    public GamerController(GamerDao gamerDao, RoomDao roomDao, VoteDao voteDao, GameService gameService, VoteService voteService) {
        this.gamerDao = gamerDao;
        this.roomDao = roomDao;
        this.voteDao = voteDao;
        this.gameService = gameService;
        this.voteService = voteService;
        gameService.initWords();
    }

    @RequestMapping(value = "/join", method = RequestMethod.GET)
    public ModelAndView join(@RequestParam(value = "roomToken", required = false) String roomToken, HttpServletRequest httpServletRequest) throws NotFoundException {
        HttpSession httpSession = httpServletRequest.getSession();
        Gamer gamer = gamerDao.findOneBySessionAndRoom(httpSession.getId(),roomToken);

        // if user is logged, redirect to room
        if(gamer != null){
            return new ModelAndView("redirect:/room?roomToken=" + roomToken);
        }

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("roomToken", roomToken);
        modelAndView.setViewName("gamer/join");
        return modelAndView;
    }

    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public String doLogin(@RequestParam String name, @RequestParam String roomToken, HttpServletRequest httpServletRequest) throws NotFoundException {
        Room room = roomDao.findOneByRoomToken(roomToken);
        if (room == null) {
            throw new NotFoundException("can't find room");
        }

        HttpSession httpSession = httpServletRequest.getSession();
        Gamer gamer = gamerDao.findOneByGamerAndRoom(name,roomToken);

        if (gamer == null) {
            if (name == "") {
                name = "anonymous";
            }
            gamer = new Gamer(name, httpSession.getId());
            gamer.setRoom(roomToken);
        }else{
            gamer.setSession(httpSession.getId());
        }

        gamerDao.save(gamer);
        return "redirect:/room?roomToken=" + roomToken;
    }

    @RequestMapping(value = "/room", method = RequestMethod.GET)
    public ModelAndView room(@RequestParam String roomToken, HttpServletRequest httpServletRequest, ModelAndView modelAndView) throws Exception {
        HttpSession httpSession = httpServletRequest.getSession();
        Gamer gamer = gamerDao.findOneBySessionAndRoom(httpSession.getId(),roomToken);

        List<Gamer> others = gamerDao.findByRoom(roomToken);
        Room room = getRoom(roomToken);

        if(gamer == null){
            return new ModelAndView("redirect:/join?roomToken="+roomToken);
        }

        modelAndView.addObject("status", room.getStatus());
        modelAndView.addObject("player", gamer);
        modelAndView.addObject ("others", others);
        modelAndView.addObject ("room", room);

        modelAndView.setViewName("gamer/room");
        return modelAndView;
    }

    private Room getRoom(@RequestParam String roomToken) throws Exception {
        Room room = roomDao.findOneByRoomToken(roomToken);

        if(room == null){
            throw new Exception("token expired!");
        }
        return room;
    }

    @RequestMapping(value = "/vote", method = RequestMethod.GET)
    public String vote(@RequestParam String roomToken, @RequestParam String voted,HttpServletRequest httpServletRequest) {
        HttpSession httpSession = httpServletRequest.getSession();
        Gamer gamer = gamerDao.findOneBySessionAndRoom(httpSession.getId(),roomToken);
        String referer = httpServletRequest.getHeader("Referer");

        if(gamer == null){
            return "redirect:/join?roomToken="+roomToken;
        }
        voteService.vote(gamer.getGamer(), voted);

        return "redirect:"+referer;
    }
}
