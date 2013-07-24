/***
 * Copyright (c) 2009 Caelum - www.caelum.com.br/opensource
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package br.com.caelum.vraptor.musicjungle.controller;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import br.com.caelum.vraptor.musicjungle.dao.UserDao;
import br.com.caelum.vraptor.musicjungle.enums.MusicType;
import br.com.caelum.vraptor.musicjungle.interceptor.Public;
import br.com.caelum.vraptor.musicjungle.interceptor.UserInfo;
import br.com.caelum.vraptor.musicjungle.model.User;
import br.com.caelum.vraptor4.Get;
import br.com.caelum.vraptor4.Path;
import br.com.caelum.vraptor4.Post;
import br.com.caelum.vraptor4.Result;
import br.com.caelum.vraptor4.Validator;
import br.com.caelum.vraptor4.validator.Validations;

import com.google.common.base.Objects;

/**
 * The resource <code>UsersController</code> handles all user 
 * operations, such as adding new users, listing users, and so on.
 */
@Resource
public class UsersController {

    private final Validator validator;
    private final Result result;
    private final UserInfo userInfo;
	private final UserDao dao;

	/**
	 * Receives dependencies through the constructor.
	 * 
	 * @param factory dao factory.
	 * @param userInfo info on the logged user.
	 * @param result VRaptor result handler.
	 * @param validator VRaptor validator.
	 */
	public UsersController(UserDao dao, UserInfo userInfo, 
			Result result, Validator validator) {
		
		this.dao = dao;
		this.result = result;
		this.validator = validator;
        this.userInfo = userInfo;
	}

	/**
	 * Accepts HTTP GET requests.
	 * 
	 * URL:  /home
	 * View: /WEB-INF/jsp/user/home.jsp
	 *
	 * Shows user's home page containing his Music collection.
	 */
	@Get("/")
	public void home() {
	    dao.refresh(userInfo.getUser());
	    result.include("musicTypes", MusicType.values());
	}

	/**
     * Accepts HTTP GET requests.
     * 
     * URL:  /users (only GET requests for this URL)
     * View: /WEB-INF/jsp/user/list.jsp
     *
     * Lists all users.
     */
	@Get("/users")
	public void list() {
        List<User> users = this.dao.listAll();
        users = Objects.firstNonNull(users, new ArrayList<User>());
        result.include("users", users);
    }

	/**
	 * Accepts HTTP POST requests.
	 * 
	 * URL:	 /users
	 * View: /WEB-INF/jsp/user/add.jsp
	 *
	 * The "user" parameter will be populated with the request 
	 * parameters, for example:
	 *
	 * POST	/user
	 * user.name=Nico
	 * user.login=555555
	 *
	 * automatically populates the name and login parameters on 
	 * the user object with values Nico and 555555.
	 *
	 * Adds new users to the database.
	 */
	@Path("/users")
	@Post
	@Public
	public void add(final User user) {
		// will add all validation errors from Hibernate Validator
		validator.validate(user); 
		
	    validator.checking(new Validations() {{
		    // checks if there is already an user with the specified login
		    boolean loginDoesNotExist = !dao.containsUserWithLogin(user.getLogin());
		    that(loginDoesNotExist, "login", "login_already_exists");
		    that(user.getLogin().matches("[a-z0-9_]+"), "login", "invalid_login");
		}});

		// redirects to the index page if any validation errors occur.
		validator.onErrorUsePageOf(HomeController.class).login();
		this.dao.add(user);

		// you can add objects to result even in redirects. Added objects will
		// survive one more request when redirecting.
		result.include("notice", "User " + user.getName() + " successfully added");
		result.redirectTo(HomeController.class).login();
	}

	/**
	 * Accepts HTTP GET requests.
	 * 
	 * URL:  /users/{login} (for example, /users/john 
	 * shows information of the user with login john)
	 * View: /WEB-INF/jsp/user/view.jsp
	 *
	 * Shows information on the specified user.
	 * @param user
	 */
	@Path("/users/{user.login}")
	@Get
	public void view(User user) {
	    result.include("user", dao.find(user.getLogin()));
	}

}
