package org.yearup.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.yearup.data.ProfileDao;
import org.yearup.data.UserDao;
import org.yearup.models.Profile;
import org.yearup.models.User;

import java.security.Principal;

@RestController
@RequestMapping("profile")
@PreAuthorize("hasAnyRole('ROLE_USER','ROLE_ADMIN')")
@CrossOrigin
public class ProfileController {

    private ProfileDao profileDao;
    private UserDao userDao;

    public ProfileController(ProfileDao profileDao, UserDao userDao)
    {
        this.profileDao = profileDao;
        this.userDao = userDao;
    }

    @GetMapping
    public Profile getProfile(Principal principal)
    {
        try
        {
            int userId = getUserId(principal);
            return profileDao.getProfile(userId);
        }
        catch(Exception e)
        {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Oops... our bad.");
        }
    }

    @PutMapping
    public Profile updateProfile(@RequestBody Profile profile, Principal principal)
    {
        try
        {
            int userId = getUserId(principal);
            profileDao.update(userId, profile);
            return profileDao.getProfile(userId);
        }
        catch(Exception e)
        {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Oops... our bad.");
        }
    }

    private int getUserId(Principal principal)
    {
        String userName = principal.getName();
        User user = userDao.getByUserName(userName);
        if(user == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found.");
        return user.getId();
    }
}
